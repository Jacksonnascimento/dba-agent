package com.dbaagent.api.controllers;

import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent/tasks")
public class AgentTaskController {

    private final OptimizationSuggestionRepository suggestionRepository;

    public AgentTaskController(OptimizationSuggestionRepository suggestionRepository) {
        this.suggestionRepository = suggestionRepository;
    }

    // O agente pergunta: "Tem trabalho aprovado pra mim?"
    @GetMapping
    public ResponseEntity<List<OptimizationSuggestion>> getApprovedTasks(@AuthenticationPrincipal AgentToken agent) {
        List<OptimizationSuggestion> tasks = suggestionRepository.findByTenantAndStatus(
                agent.getTenant(), SuggestionStatus.APPROVED
        );
        return ResponseEntity.ok(tasks);
    }

    // O agente avisa: "Terminei de rodar o script!"
    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long id, @AuthenticationPrincipal AgentToken agent) {
        OptimizationSuggestion task = suggestionRepository.findById(id).orElse(null);

        // Verifica se a tarefa existe e se realmente pertence à empresa deste Agente (Segurança em 1º lugar!)
        if (task == null || !task.getTenant().getId().equals(agent.getTenant().getId())) {
            return ResponseEntity.status(403).body(Map.of("erro", "Tarefa não encontrada ou não pertence a este agente."));
        }

        task.setStatus(SuggestionStatus.EXECUTED);
        task.setAppliedAt(LocalDateTime.now());
        suggestionRepository.save(task);

        return ResponseEntity.ok(Map.of("mensagem", "Tarefa marcada como EXECUTED com sucesso!"));
    }
}