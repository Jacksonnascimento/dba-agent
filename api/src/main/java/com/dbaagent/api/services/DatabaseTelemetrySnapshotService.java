package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AgentTelemetryRequestDTO;
import com.dbaagent.api.dtos.DatabaseTelemetrySnapshotResponseDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.DatabaseTelemetrySnapshot;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.DatabaseTelemetrySnapshotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class DatabaseTelemetrySnapshotService {

    private static final int MAX_DMV = 12000;
    private static final int MAX_WAIT = 12000;
    private static final int MAX_TOP_QUERIES = 20000;
    private static final int MAX_INDEX = 12000;
    private static final int MAX_EXECUTION_PLANS = 60000;

    private final DatabaseTelemetrySnapshotRepository repository;

    public DatabaseTelemetrySnapshotService(DatabaseTelemetrySnapshotRepository repository) {
        this.repository = repository;
    }

    public DatabaseTelemetrySnapshot persistSnapshot(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            AgentTelemetryRequestDTO telemetry) {

        String ddl = telemetry.getSchemaDdl() != null ? telemetry.getSchemaDdl().trim() : "";
        String schemaHash = sha256Hex(ddl);

        // Context hash inclui métricas e planos para evitar cache “errado” por DDL igual
        String contextHash = sha256Hex(
                ddl +
                        "\n--dmv--\n" + nullSafe(telemetry.getDmvStats()) +
                        "\n--waits--\n" + nullSafe(telemetry.getWaitStats()) +
                        "\n--topq--\n" + nullSafe(telemetry.getTopQueries()) +
                        "\n--plans--\n" + nullSafe(telemetry.getExecutionPlans()) +
                        "\n--indexes--\n" + nullSafe(telemetry.getIndexStats()));

        DatabaseTelemetrySnapshot snapshot = new DatabaseTelemetrySnapshot();
        snapshot.setTenant(tenant);
        snapshot.setDatabaseConnection(databaseConnection);
        snapshot.setDbEngine(StringUtils.hasText(telemetry.getDbEngine()) ? telemetry.getDbEngine() : databaseConnection.getDbEngine());
        snapshot.setSchemaDdl(ddl);
        snapshot.setSchemaHash(schemaHash);
        snapshot.setContextHash(contextHash);
        snapshot.setDmvStats(limit(telemetry.getDmvStats(), MAX_DMV));
        snapshot.setWaitStats(limit(telemetry.getWaitStats(), MAX_WAIT));
        snapshot.setTopQueries(limit(telemetry.getTopQueries(), MAX_TOP_QUERIES));
        snapshot.setExecutionPlans(limit(telemetry.getExecutionPlans(), MAX_EXECUTION_PLANS));
        snapshot.setIndexStats(limit(telemetry.getIndexStats(), MAX_INDEX));
        return repository.save(snapshot);
    }

    public Page<DatabaseTelemetrySnapshotResponseDTO> listSnapshots(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            Pageable pageable) {
        return repository.findByTenantAndDatabaseConnectionOrderByCollectedAtDesc(tenant, databaseConnection, pageable)
                .map(this::toDto);
    }

    private DatabaseTelemetrySnapshotResponseDTO toDto(DatabaseTelemetrySnapshot s) {
        return DatabaseTelemetrySnapshotResponseDTO.builder()
                .id(s.getId())
                .schemaHash(s.getSchemaHash())
                .contextHash(s.getContextHash())
                .dbEngine(s.getDbEngine())
                .dmvStats(s.getDmvStats())
                .waitStats(s.getWaitStats())
                .topQueries(s.getTopQueries())
                .executionPlans(s.getExecutionPlans())
                .indexStats(s.getIndexStats())
                .collectedAt(s.getCollectedAt())
                .build();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String limit(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max) + "\n-- truncated by backend --";
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

