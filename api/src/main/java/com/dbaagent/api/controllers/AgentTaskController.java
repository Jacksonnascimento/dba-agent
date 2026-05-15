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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    @Transactional(readOnly = true) // Mantém a sessão aberta para evitar o LazyInitializationException
    public ResponseEntity<List<AgentTaskResponseDTO>> listApprovedTasks() {
        AgentToken agentToken = requireAgentToken();
        Tenant tenant = agentToken.getTenant();
        List<OptimizationSuggestion> tasks = repository.findByTenantAndDatabaseConnectionAndStatusIn(
                tenant,
                agentToken.getDatabaseConnection(),
                List.of(SuggestionStatus.APPROVED, SuggestionStatus.ROLLBACK_PENDING));
        
        if (tasks.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        
        List<AgentTaskResponseDTO> body = tasks.stream().map(this::toAgentTaskDto).toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/complete")
    @Transactional // Garante consistência entre atualizar a sugestão e gravar a auditoria
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
        if (suggestion.getStatus() != SuggestionStatus.APPROVED && suggestion.getStatus() != SuggestionStatus.ROLLBACK_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Apenas tarefas APPROVED ou ROLLBACK_PENDING podem ser marcadas como completas.");
        }

        String auditAction;
        if (suggestion.getStatus() == SuggestionStatus.APPROVED) {
            suggestion.setAppliedAt(LocalDateTime.now());
            suggestion.setStatus(SuggestionStatus.EXECUTED);
            auditAction = "EXECUTED";
        } else {
            suggestion.setStatus(SuggestionStatus.ROLLED_BACK);
            auditAction = "ROLLED_BACK";
        }
        repository.save(suggestion);
        
        auditLogService.log(
                suggestion,
                auditAction,
                "AGENT",
                "agentToken:" + agentToken.getId(),
                "Ação concluída pelo agente");

        return ResponseEntity.ok("Task marcada como concluída pelo Agente.");
    }

    @PostMapping("/{id}/fail")
    @Transactional
    public ResponseEntity<String> markTaskAsFailed(@PathVariable Long id, @RequestBody(required = false) Map<String, String> payload) {
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

        String errorMsg = payload != null && payload.get("error") != null ? payload.get("error") : "Erro desconhecido";
        String auditAction;

        if (suggestion.getStatus() == SuggestionStatus.APPROVED) {
            suggestion.setStatus(SuggestionStatus.FAILED);
            auditAction = "FAILED";
        } else if (suggestion.getStatus() == SuggestionStatus.ROLLBACK_PENDING) {
            suggestion.setStatus(SuggestionStatus.ROLLBACK_FAILED);
            auditAction = "ROLLBACK_FAILED";
        } else {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Task não está em estado executável.");
        }

        repository.save(suggestion);
        auditLogService.log(
                suggestion,
                auditAction,
                "AGENT",
                "agentToken:" + agentToken.getId(),
                "Falha na execução: " + errorMsg);

        return ResponseEntity.ok("Task marcada como falha pelo Agente.");
    }

    private AgentTaskResponseDTO toAgentTaskDto(OptimizationSuggestion s) {
        String hash = s.getSchemaHash() != null ? s.getSchemaHash() : "";
        String taskType = s.getStatus() == SuggestionStatus.ROLLBACK_PENDING ? "ROLLBACK" : "EXECUTE";
        
        if ("FORCE_TELEMETRY".equals(hash)) {
            taskType = "FORCE_TELEMETRY";
        }
        
        return AgentTaskResponseDTO.builder()
                .id(s.getId())
                .databaseConnectionId(s.getDatabaseConnection().getId())
                .databaseName(s.getDatabaseConnection().getName())
                .schemaHash(hash)
                .diagnosis(s.getSuggestionText())
                .upScript(s.getUpScript())
                .downScript(s.getDownScript())
                .status(s.getStatus().name())
                .taskType(taskType)
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