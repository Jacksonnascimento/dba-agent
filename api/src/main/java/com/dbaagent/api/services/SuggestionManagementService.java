package com.dbaagent.api.services;

import com.dbaagent.api.dtos.SuggestionResponseDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuggestionManagementService {

    private final OptimizationSuggestionRepository repository;
    private final DatabaseConnectionRepository databaseConnectionRepository;
    private final SuggestionAuditLogService auditLogService;

    public SuggestionManagementService(
            OptimizationSuggestionRepository repository,
            DatabaseConnectionRepository databaseConnectionRepository,
            SuggestionAuditLogService auditLogService) {
        this.repository = repository;
        this.databaseConnectionRepository = databaseConnectionRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponseDTO> listPending(Tenant tenant, Long databaseConnectionId) {
        List<OptimizationSuggestion> suggestions;
        if (databaseConnectionId == null) {
            suggestions = repository.findByTenantAndStatus(tenant, SuggestionStatus.PENDING);
        } else {
            DatabaseConnection databaseConnection = databaseConnectionRepository
                    .findByIdAndTenant(databaseConnectionId, tenant)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Banco não encontrado para o tenant informado."));
            suggestions = repository.findByTenantAndDatabaseConnectionAndStatus(
                    tenant,
                    databaseConnection,
                    SuggestionStatus.PENDING);
        }
        
        return suggestions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponseDTO> listExecuted(Tenant tenant, Long databaseConnectionId) {
        List<OptimizationSuggestion> suggestions;
        List<SuggestionStatus> finalStatuses = List.of(
                SuggestionStatus.EXECUTED,
                SuggestionStatus.FAILED,
                SuggestionStatus.ROLLBACK_PENDING,
                SuggestionStatus.ROLLED_BACK,
                SuggestionStatus.ROLLBACK_FAILED);

        if (databaseConnectionId == null) {
            suggestions = repository.findAll().stream() // fallback since no findByTenantAndStatusIn in repo yet, let's filter manually
                    .filter(s -> s.getTenant().getId().equals(tenant.getId()))
                    .filter(s -> finalStatuses.contains(s.getStatus()))
                    .collect(Collectors.toList());
        } else {
            DatabaseConnection databaseConnection = databaseConnectionRepository
                    .findByIdAndTenant(databaseConnectionId, tenant)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Banco não encontrado para o tenant informado."));
            suggestions = repository.findByTenantAndDatabaseConnectionAndStatusIn(
                    tenant,
                    databaseConnection,
                    finalStatuses);
        }
        
        return suggestions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SuggestionResponseDTO approve(Long id, Tenant tenant, User user) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        // Trava de Segurança Multi-Tenant
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }
        
        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente sugestões PENDING podem ser aprovadas.");
        }

        suggestion.setStatus(SuggestionStatus.APPROVED);
        repository.save(suggestion);
        auditLogService.log(
                suggestion,
                "APPROVED",
                "USER",
                "user:" + user.getId(),
                "Sugestão aprovada no painel");
        
        return mapToDTO(suggestion);
    }

    @Transactional
    public SuggestionResponseDTO reject(Long id, Tenant tenant, User user) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }

        if (suggestion.getStatus() != SuggestionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente sugestões PENDING podem ser rejeitadas.");
        }

        suggestion.setStatus(SuggestionStatus.REJECTED);
        repository.save(suggestion);
        auditLogService.log(
                suggestion,
                "REJECTED",
                "USER",
                "user:" + user.getId(),
                "Sugestão rejeitada no painel");
        
        return mapToDTO(suggestion);
    }

    @Transactional
    public SuggestionResponseDTO requestRollback(Long id, Tenant tenant, User user) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }

        if (suggestion.getStatus() != SuggestionStatus.EXECUTED && suggestion.getStatus() != SuggestionStatus.ROLLBACK_FAILED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Apenas sugestões EXECUTED ou ROLLBACK_FAILED podem sofrer Rollback.");
        }

        suggestion.setStatus(SuggestionStatus.ROLLBACK_PENDING);
        repository.save(suggestion);
        auditLogService.log(
                suggestion,
                "ROLLBACK_REQUESTED",
                "USER",
                "user:" + user.getId(),
                "Rollback solicitado pelo usuário");
        
        return mapToDTO(suggestion);
    }

    private SuggestionResponseDTO mapToDTO(OptimizationSuggestion suggestion) {
        return SuggestionResponseDTO.builder()
                .id(suggestion.getId())
                .databaseConnectionId(suggestion.getDatabaseConnection().getId())
                .databaseConnectionName(suggestion.getDatabaseConnection().getName())
                .databaseName(suggestion.getDatabaseName())
                .tableName(suggestion.getTableName())
                .suggestionText(suggestion.getSuggestionText())
                .upScript(suggestion.getUpScript())
                .downScript(suggestion.getDownScript())
                .status(suggestion.getStatus().name())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}