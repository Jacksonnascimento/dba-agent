package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.TenantSettingsResponseDTO;
import com.dbaagent.api.dtos.TenantSettingsUpdateDTO;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.TenantRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.dbaagent.api.services.GeminiIntegrationService;
import com.dbaagent.api.services.ClaudeIntegrationService;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tenant/settings")
public class TenantSettingsController {

    private final TenantRepository tenantRepository;
    private final GeminiIntegrationService geminiIntegrationService;
    private final ClaudeIntegrationService claudeIntegrationService;

    public TenantSettingsController(TenantRepository tenantRepository, 
                                    GeminiIntegrationService geminiIntegrationService,
                                    ClaudeIntegrationService claudeIntegrationService) {
        this.tenantRepository = tenantRepository;
        this.geminiIntegrationService = geminiIntegrationService;
        this.claudeIntegrationService = claudeIntegrationService;
    }

    @PostMapping("/ai-test")
    public ResponseEntity<?> testAiConnection(@AuthenticationPrincipal User user) {
        Tenant tenant = tenantRepository.findById(user.getTenant().getId())
                .orElseThrow(() -> new RuntimeException("Tenant não encontrado"));

        String provider = tenant.getAiProvider() != null ? tenant.getAiProvider() : "GEMINI";

        try {
            if ("CLAUDE".equalsIgnoreCase(provider)) {
                String key = tenant.getClaudeApiKey();
                if (key == null || key.isBlank()) throw new IllegalArgumentException("Chave Claude não configurada.");
                String model = tenant.getAiModel() != null && !tenant.getAiModel().isBlank() ? tenant.getAiModel() : "claude-3-haiku-20240307";
                claudeIntegrationService.testConnection(key, model);
            } else {
                String key = tenant.getGeminiApiKey();
                if (key == null || key.isBlank()) throw new IllegalArgumentException("Chave Gemini não configurada.");
                String model = tenant.getAiModel() != null && !tenant.getAiModel().isBlank() ? tenant.getAiModel() : "gemini-1.5-flash";
                geminiIntegrationService.testConnection(key, model);
            }
            return ResponseEntity.ok(Map.of("message", "Conexão bem-sucedida com " + provider + "!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<TenantSettingsResponseDTO> get(@AuthenticationPrincipal User user) {
        Tenant tenant = tenantRepository.findById(user.getTenant().getId())
                .orElseThrow(() -> new RuntimeException("Tenant não encontrado"));
        return ResponseEntity.ok(map(tenant));
    }

    @PutMapping("/ai-config")
    public ResponseEntity<TenantSettingsResponseDTO> updateAiConfig(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TenantSettingsUpdateDTO request) {
        Tenant tenant = tenantRepository.findById(user.getTenant().getId())
                .orElseThrow(() -> new RuntimeException("Tenant não encontrado"));
        
        if (request.getAiProvider() != null && !request.getAiProvider().isBlank()) {
            tenant.setAiProvider(request.getAiProvider().trim().toUpperCase());
        }
        
        if (request.getAiModel() != null) {
            tenant.setAiModel(request.getAiModel().trim());
        }

        if (request.getGeminiApiKey() != null && !request.getGeminiApiKey().isBlank()) {
            tenant.setGeminiApiKey(request.getGeminiApiKey().trim());
        }
        
        if (request.getClaudeApiKey() != null && !request.getClaudeApiKey().isBlank()) {
            tenant.setClaudeApiKey(request.getClaudeApiKey().trim());
        }

        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.ok(map(saved));
    }

    private TenantSettingsResponseDTO map(Tenant tenant) {
        String geminiKey = tenant.getGeminiApiKey();
        String claudeKey = tenant.getClaudeApiKey();
        
        return TenantSettingsResponseDTO.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .aiProvider(tenant.getAiProvider())
                .aiModel(tenant.getAiModel())
                .geminiApiKeyConfigured(geminiKey != null && !geminiKey.isBlank())
                .geminiApiKeyMasked(mask(geminiKey))
                .claudeApiKeyConfigured(claudeKey != null && !claudeKey.isBlank())
                .claudeApiKeyMasked(mask(claudeKey))
                .build();
    }

    private static String mask(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.length() <= 8) {
            return "********";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}

