package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentTelemetryRequestDTO;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import com.dbaagent.api.repositories.TenantRepository;
import com.dbaagent.api.services.GeminiIntegrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent/telemetry")
public class AgentTelemetryController {

    private final GeminiIntegrationService geminiIntegrationService;
    private final OptimizationSuggestionRepository repository;
    private final TenantRepository tenantRepository;

    public AgentTelemetryController(GeminiIntegrationService geminiIntegrationService, 
                                    OptimizationSuggestionRepository repository,
                                    TenantRepository tenantRepository) {
        this.geminiIntegrationService = geminiIntegrationService;
        this.repository = repository;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping
    public ResponseEntity<String> receiveTelemetry(@RequestBody AgentTelemetryRequestDTO telemetry) {
        // Em produção, o Tenant virá do AgentToken validado no AgentAuthenticationFilter.
        // Como fallback momentâneo, pegamos o Tenant base (Horizon AJ) para não quebrar a inserção.
        Tenant tenant = tenantRepository.findAll().stream().findFirst()
            .orElseThrow(() -> new RuntimeException("Nenhum Tenant configurado no sistema."));

        // Sua lógica original de processamento de IA entra aqui.
        // Exemplo: AiAnalysisResultDTO result = geminiIntegrationService.analyze(telemetry);

        String upScript = "-- Script gerado pelo Linter/IA";
        String downScript = "-- Rollback gerado pelo Linter/IA";
        String suggestionText = "Análise processada a partir da telemetria do Agente.";

        // CORREÇÃO: Uso do Builder injetando o Tenant e os campos da arquitetura
        OptimizationSuggestion suggestion = OptimizationSuggestion.builder()
                .tenant(tenant)
                .databaseName("db_cliente") // Extraia do seu 'telemetry' DTO
                .tableName("tb_lenta")      // Extraia do seu 'telemetry' DTO
                .suggestionText(suggestionText)
                .upScript(upScript)
                .downScript(downScript)
                .status(SuggestionStatus.PENDING)
                .build();

        repository.save(suggestion);

        return ResponseEntity.ok("Telemetria processada e sugestão salva com status PENDING.");
    }
}