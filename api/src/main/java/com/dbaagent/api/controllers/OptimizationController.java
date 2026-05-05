package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.OptimizationRequestDTO;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import com.dbaagent.api.services.GeminiIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/optimizations")
public class OptimizationController {

    private final GeminiIntegrationService geminiIntegrationService;
    private final OptimizationSuggestionRepository repository;

    public OptimizationController(GeminiIntegrationService geminiIntegrationService, OptimizationSuggestionRepository repository) {
        this.geminiIntegrationService = geminiIntegrationService;
        this.repository = repository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<OptimizationSuggestion> analyze(
            @RequestBody OptimizationRequestDTO requestDTO,
            @AuthenticationPrincipal User authenticatedUser) {

        // O usuário logado dispara a análise manual. O Tenant é extraído dele.
        String upScript = "-- Script otimizado";
        String downScript = "-- Script reversão";
        String suggestionText = "Análise gerada sob demanda via API.";

        // CORREÇÃO: Uso do Builder injetando o Tenant do usuário que fez a requisição
        OptimizationSuggestion suggestion = OptimizationSuggestion.builder()
                .tenant(authenticatedUser.getTenant())
                .databaseName("db_analise") // Ajuste conforme seu DTO
                .tableName("tb_analise")    // Ajuste conforme seu DTO
                .suggestionText(suggestionText)
                .upScript(upScript)
                .downScript(downScript)
                .status(SuggestionStatus.PENDING)
                .build();

        OptimizationSuggestion saved = repository.save(suggestion);
        return ResponseEntity.ok(saved);
    }
}