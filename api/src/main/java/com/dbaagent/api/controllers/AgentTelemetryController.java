package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentTelemetryRequestDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.DatabaseTelemetrySnapshot;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.AgentWorkerRepository;
import com.dbaagent.api.services.DatabaseTelemetrySnapshotService;
import com.dbaagent.api.services.OptimizationAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/agent/telemetry")
public class AgentTelemetryController {

    private final OptimizationAnalysisService optimizationAnalysisService;
    private final DatabaseTelemetrySnapshotService snapshotService;
    private final AgentWorkerRepository agentWorkerRepository;

    public AgentTelemetryController(
            OptimizationAnalysisService optimizationAnalysisService,
            DatabaseTelemetrySnapshotService snapshotService,
            AgentWorkerRepository agentWorkerRepository) {
        this.optimizationAnalysisService = optimizationAnalysisService;
        this.snapshotService = snapshotService;
        this.agentWorkerRepository = agentWorkerRepository;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> receiveTelemetry(
            @Valid @RequestBody AgentTelemetryRequestDTO telemetry,
            @RequestHeader(value = "X-Worker-Token", required = false) String workerTokenHeader) {
        AgentToken agentToken = requireAgentToken();
        Tenant tenant = agentToken.getTenant();
        DatabaseConnection databaseConnection = agentToken.getDatabaseConnection();

        AgentWorker agentWorker = resolveExecutingWorker(workerTokenHeader, tenant, databaseConnection);

        DatabaseTelemetrySnapshot snapshot = snapshotService.persistSnapshot(
                tenant,
                databaseConnection,
                telemetry);

        OptimizationSuggestion saved = optimizationAnalysisService.analyzeAndPersistFromSnapshot(
                tenant,
                databaseConnection,
                agentWorker,
                snapshot.getSchemaDdl(),
                snapshot.getDmvStats(),
                snapshot.getWaitStats(),
                snapshot.getTopQueries(),
                snapshot.getExecutionPlans(),
                snapshot.getIndexStats(),
                snapshot.getDbEngine(),
                null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Telemetria processada. Sugestão id=" + saved.getId() + " status=PENDING.");
    }

    private AgentWorker resolveExecutingWorker(
            String workerTokenHeader,
            Tenant tenant,
            DatabaseConnection databaseConnection) {
        if (!StringUtils.hasText(workerTokenHeader)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Header X-Worker-Token é obrigatório para identificar o agente em execução.");
        }

        AgentWorker worker = agentWorkerRepository.findByWorkerTokenWithDatabases(workerTokenHeader.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Worker Token inválido."));

        if (!worker.getTenant().getId().equals(tenant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Worker não pertence ao tenant do banco.");
        }

        boolean linked = worker.getDatabases().stream()
                .anyMatch(db -> db.getId().equals(databaseConnection.getId()));
        if (!linked) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Este banco não está vinculado ao agente informado.");
        }

        return worker;
    }

    private static AgentToken requireAgentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AgentToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agente não autenticado.");
        }
        return (AgentToken) auth.getPrincipal();
    }
}
