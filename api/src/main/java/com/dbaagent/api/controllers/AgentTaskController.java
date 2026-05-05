package com.dbaagent.api.controllers;

import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/agent/tasks")
public class AgentTaskController {

    private final OptimizationSuggestionRepository repository;

    public AgentTaskController(OptimizationSuggestionRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<String> markTaskAsComplete(@PathVariable Long id) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task/Sugestão não encontrada para o ID: " + id));

        // CORREÇÃO: O lombok vai prover este método caso você tenha executado o Clean Workspace
        suggestion.setAppliedAt(LocalDateTime.now());
        
        // Se houver um status como 'APPLIED' no seu Enum, você pode alterá-lo aqui.
        repository.save(suggestion);

        return ResponseEntity.ok("Task marcada como aplicada pelo Agente.");
    }
}