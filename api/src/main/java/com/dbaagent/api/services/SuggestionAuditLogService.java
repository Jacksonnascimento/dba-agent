package com.dbaagent.api.services;

import com.dbaagent.api.dtos.SuggestionAuditLogResponseDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.SuggestionAuditLog;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.repositories.SuggestionAuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SuggestionAuditLogService {

    private final SuggestionAuditLogRepository repository;
    private final DatabaseConnectionRepository databaseConnectionRepository;

    public SuggestionAuditLogService(
            SuggestionAuditLogRepository repository,
            DatabaseConnectionRepository databaseConnectionRepository) {
        this.repository = repository;
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    public void log(
            OptimizationSuggestion suggestion,
            String action,
            String actorType,
            String actorIdentifier,
            String details) {
        SuggestionAuditLog log = new SuggestionAuditLog();
        log.setTenant(suggestion.getTenant());
        log.setDatabaseConnection(suggestion.getDatabaseConnection());
        log.setSuggestion(suggestion);
        log.setAction(action);
        log.setActorType(actorType);
        log.setActorIdentifier(actorIdentifier);
        log.setDetails(details);
        repository.save(log);
    }

    public Page<SuggestionAuditLogResponseDTO> list(
            Tenant tenant,
            Long databaseConnectionId,
            Pageable pageable) {
        if (databaseConnectionId == null) {
            return repository.findByTenantOrderByCreatedAtDesc(tenant, pageable).map(this::map);
        }
        DatabaseConnection db = databaseConnectionRepository
                .findByIdAndTenant(databaseConnectionId, tenant)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Banco não encontrado para o tenant informado."));
        return repository.findByTenantAndDatabaseConnectionOrderByCreatedAtDesc(tenant, db, pageable).map(this::map);
    }

    private SuggestionAuditLogResponseDTO map(SuggestionAuditLog l) {
        return SuggestionAuditLogResponseDTO.builder()
                .id(l.getId())
                .suggestionId(l.getSuggestion().getId())
                .databaseConnectionId(l.getDatabaseConnection().getId())
                .databaseConnectionName(l.getDatabaseConnection().getName())
                .action(l.getAction())
                .actorType(l.getActorType())
                .actorIdentifier(l.getActorIdentifier())
                .details(l.getDetails())
                .createdAt(l.getCreatedAt())
                .build();
    }
}

