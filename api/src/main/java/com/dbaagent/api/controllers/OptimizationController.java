package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.OptimizationRequestDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.services.OptimizationAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/optimizations")
public class OptimizationController {

    private final OptimizationAnalysisService optimizationAnalysisService;
    private final DatabaseConnectionRepository databaseConnectionRepository;

    public OptimizationController(
            OptimizationAnalysisService optimizationAnalysisService,
            DatabaseConnectionRepository databaseConnectionRepository) {
        this.optimizationAnalysisService = optimizationAnalysisService;
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<OptimizationSuggestion> analyze(
            @Valid @RequestBody OptimizationRequestDTO requestDTO,
            @AuthenticationPrincipal User authenticatedUser) {

        DatabaseConnection databaseConnection = databaseConnectionRepository
                .findByIdAndTenant(requestDTO.getDatabaseConnectionId(), authenticatedUser.getTenant())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Banco não encontrado para o tenant informado."));

        OptimizationSuggestion saved = optimizationAnalysisService.analyzeAndPersist(
                authenticatedUser.getTenant(),
                databaseConnection,
                null,
                requestDTO.getSchemaDdl(),
                requestDTO.getDmvStats(),
                requestDTO.getDbEngine(),
                requestDTO.getAiModel());

        return ResponseEntity.ok(saved);
    }
}