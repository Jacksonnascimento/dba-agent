package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.OptimizationRequestDTO;
import com.dbaagent.api.dtos.OptimizationResponseDTO;
import com.dbaagent.api.dtos.AiAnalysisResultDTO;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.SemanticCache;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.entities.Tenant;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/optimizations")
public class OptimizationController {

    private final SemanticCacheService semanticCacheService;
    private final LinterService linterService;
    private final GeminiIntegrationService geminiService;
    private final OptimizationSuggestionRepository suggestionRepository;

    public OptimizationController(SemanticCacheService semanticCacheService, 
                                  LinterService linterService, 
                                  GeminiIntegrationService geminiService,
                                  OptimizationSuggestionRepository suggestionRepository) {
        this.semanticCacheService = semanticCacheService;
        this.linterService = linterService;
        this.geminiService = geminiService;
        this.suggestionRepository = suggestionRepository;
    }

    @PostMapping("/analyze")
    public ResponseEntity<OptimizationResponseDTO> analyzeDatabase(
            @Valid @RequestBody OptimizationRequestDTO request,
            @AuthenticationPrincipal User loggedUser) { // 🧙‍♂️ Mágica do Spring Security aqui!

        Tenant tenant = loggedUser.getTenant();
        String apiKey = tenant.getGeminiApiKey();

        // 1. Validação do Cofre de Chaves
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new OptimizationResponseDTO(
                    "A empresa ainda não configurou a chave da API do Gemini.", false, "Sistema"
            ));
        }

        // 2. Linter Interno
        List<String> linterFindings = linterService.runLinter(request.getSchemaDdl());
        if (!linterFindings.isEmpty()) {
            return ResponseEntity.ok(new OptimizationResponseDTO(String.join(" | ", linterFindings), false, "Linter Interno"));
        }

        String hashInput = request.getDbEngine() + ":" + request.getSchemaDdl();
        String schemaHash = generateHash(hashInput);

        // 3. Cache Semântico Isolado por Empresa
        Optional<SemanticCache> cachedResult = semanticCacheService.checkCache(schemaHash, tenant);
        if (cachedResult.isPresent()) {
            return ResponseEntity.ok(new OptimizationResponseDTO(
                    cachedResult.get().getSuggestedImprovement(), true, cachedResult.get().getAiProvider()));
        }

        // 4. Integração com a IA usando a chave do Cofre
        AiAnalysisResultDTO iaResult = geminiService.analyzeWithGemini(
                request.getSchemaDdl(), request.getDmvStats(), apiKey, request.getAiModel(), request.getDbEngine()
        );

        if (iaResult.getDiagnostico() != null && !iaResult.getDiagnostico().startsWith("Erro ao processar")) {
            
            semanticCacheService.saveToCache(schemaHash, iaResult.getDiagnostico(), "Google Gemini - " + request.getAiModel(), tenant);

            OptimizationSuggestion novaSugestao = new OptimizationSuggestion(
                    schemaHash,
                    iaResult.getDiagnostico(),
                    iaResult.getUpScript(),
                    iaResult.getDownScript()
            );
            novaSugestao.setTenant(tenant); // 🔒 Amarra a sugestão à empresa logada!
            
            novaSugestao = suggestionRepository.save(novaSugestao);

            return ResponseEntity.ok(new OptimizationResponseDTO(
                    novaSugestao.getId(),
                    iaResult.getDiagnostico(),
                    iaResult.getUpScript(),
                    iaResult.getDownScript(),
                    false,
                    "Google Gemini - " + request.getAiModel() + " (" + request.getDbEngine() + ")"
            ));
        }

        return ResponseEntity.status(500).body(new OptimizationResponseDTO(iaResult.getDiagnostico(), false, "Erro na API"));
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