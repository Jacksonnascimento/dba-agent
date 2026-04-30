package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentTelemetryRequestDTO;
import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.SemanticCache;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import com.dbaagent.api.services.GeminiIntegrationService;
import com.dbaagent.api.services.SemanticCacheService;
import com.dbaagent.api.services.linter.LinterService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/agent/telemetry")
public class AgentTelemetryController {

    private final SemanticCacheService semanticCacheService;
    private final LinterService linterService;
    private final GeminiIntegrationService geminiService;
    private final OptimizationSuggestionRepository suggestionRepository;

    public AgentTelemetryController(SemanticCacheService semanticCacheService,
                                    LinterService linterService,
                                    GeminiIntegrationService geminiService,
                                    OptimizationSuggestionRepository suggestionRepository) {
        this.semanticCacheService = semanticCacheService;
        this.linterService = linterService;
        this.geminiService = geminiService;
        this.suggestionRepository = suggestionRepository;
    }

    @PostMapping
    public ResponseEntity<?> receiveTelemetry(
            @Valid @RequestBody AgentTelemetryRequestDTO request,
            @AuthenticationPrincipal AgentToken agentToken) {

        Tenant tenant = agentToken.getTenant();
        String apiKey = tenant.getGeminiApiKey();

        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("erro", "Tenant sem chave de API configurada."));
        }

        // 1. Motor de Linter (Custo Zero)
        List<String> linterFindings = linterService.runLinter(request.getSchemaDdl());
        if (!linterFindings.isEmpty()) {
            createPendingSuggestion(
                    generateHash(request.getDbEngine() + ":" + request.getSchemaDdl()),
                    "Linter Interno: " + String.join(" | ", linterFindings),
                    "-- TODO: Aplicar correção manual baseada nas regras do Linter",
                    "-- TODO: Rollback manual",
                    tenant
            );
            return ResponseEntity.status(201).body(Map.of("mensagem", "Telemetria recebida. Análise estática (Linter) gerou pendências para aprovação."));
        }

        // Gera o Hash incluindo DDL e DMVs para ser bem preciso
        String hashInput = request.getDbEngine() + ":" + request.getSchemaDdl() + ":" + 
                           (request.getDmvStats() != null ? request.getDmvStats() : "");
        String schemaHash = generateHash(hashInput);

        // 2. Cache Semântico
        Optional<SemanticCache> cachedResult = semanticCacheService.checkCache(schemaHash, tenant);
        if (cachedResult.isPresent()) {
            createPendingSuggestion(schemaHash, cachedResult.get().getSuggestedImprovement(), 
                    "-- Script Up injetado via Cache Semântico", "-- Script Down injetado via Cache", tenant);
            return ResponseEntity.status(201).body(Map.of("mensagem", "Telemetria recebida. Cache Semântico reaproveitou análise anterior."));
        }

        // 3. Motor de IA
        AiAnalysisResultDTO iaResult = geminiService.analyzeWithGemini(
                request.getSchemaDdl(), request.getDmvStats(), apiKey, "gemini-2.5-flash", request.getDbEngine()
        );

        if (iaResult.getDiagnostico() != null && !iaResult.getDiagnostico().startsWith("Erro ao processar")) {
            semanticCacheService.saveToCache(schemaHash, iaResult.getDiagnostico(), "Google Gemini", tenant);
            createPendingSuggestion(schemaHash, iaResult.getDiagnostico(), iaResult.getUpScript(), iaResult.getDownScript(), tenant);
            return ResponseEntity.status(201).body(Map.of("mensagem", "Telemetria processada via IA. Nova sugestão enviada para a Caixa de Entrada do DBA."));
        }

        return ResponseEntity.status(500).body(Map.of("erro", "Falha na análise via IA.", "detalhes", iaResult.getDiagnostico()));
    }

    private void createPendingSuggestion(String hash, String diagnosis, String upScript, String downScript, Tenant tenant) {
        OptimizationSuggestion suggestion = new OptimizationSuggestion(hash, diagnosis, upScript, downScript);
        suggestion.setTenant(tenant);
        suggestion.setStatus(SuggestionStatus.PENDING); // A MÁGICA ACONTECE AQUI! Vai direto pra fila de aprovação humana.
        suggestionRepository.save(suggestion);
    }

    private String generateHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao gerar hash", e);
        }
    }
}