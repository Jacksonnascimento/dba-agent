package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentTaskResponseDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import com.dbaagent.api.services.SuggestionAuditLogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agent/tasks")
public class AgentTaskController {

    private final OptimizationSuggestionRepository repository;
    private final SuggestionAuditLogService auditLogService;

    public AgentTaskController(
            OptimizationSuggestionRepository repository,
            SuggestionAuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<List<AgentTaskResponseDTO>> listApprovedTasks() {
        AgentToken agentToken = requireAgentToken();
        Tenant tenant = agentToken.getTenant();
        List<OptimizationSuggestion> approved = repository.findByTenantAndDatabaseConnectionAndStatus(
                tenant,
                agentToken.getDatabaseConnection(),
                SuggestionStatus.APPROVED);
        if (approved.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        List<AgentTaskResponseDTO> body = approved.stream().map(this::toAgentTaskDto).toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<String> markTaskAsComplete(@PathVariable Long id) {
        AgentToken agentToken = requireAgentToken();
        Tenant tenant = agentToken.getTenant();
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task não encontrada: " + id));

        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task pertence a outro tenant.");
        }
        if (!suggestion.getDatabaseConnection().getId().equals(agentToken.getDatabaseConnection().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task pertence a outro banco.");
        }
        if (suggestion.getStatus() != SuggestionStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Apenas tarefas APPROVED podem ser marcadas como EXECUTED.");
        }

        suggestion.setAppliedAt(LocalDateTime.now());
        suggestion.setStatus(SuggestionStatus.EXECUTED);
        repository.save(suggestion);
        auditLogService.log(
                suggestion,
                "EXECUTED",
                "AGENT",
                "agentToken:" + agentToken.getId(),
                "Script executado pelo agente");

        return ResponseEntity.ok("Task marcada como executada pelo Agente.");
    }

    private AgentTaskResponseDTO toAgentTaskDto(OptimizationSuggestion s) {
        String hash = s.getSchemaHash() != null ? s.getSchemaHash() : "";
        return AgentTaskResponseDTO.builder()
                .id(s.getId())
                .databaseConnectionId(s.getDatabaseConnection().getId())
                .databaseName(s.getDatabaseConnection().getName())
                .schemaHash(hash)
                .diagnosis(s.getSuggestionText())
                .upScript(s.getUpScript())
                .downScript(s.getDownScript())
                .status(s.getStatus().name())
                .build();
    }

    private static AgentToken requireAgentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AgentToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agente não autenticado.");
        }
        return (AgentToken) auth.getPrincipal();
    }
}