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

@RestController
@RequestMapping("/api/v1/tenant/settings")
public class TenantSettingsController {

    private final TenantRepository tenantRepository;

    public TenantSettingsController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public ResponseEntity<TenantSettingsResponseDTO> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(map(user.getTenant()));
    }

    @PutMapping("/gemini-key")
    public ResponseEntity<TenantSettingsResponseDTO> updateGeminiKey(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TenantSettingsUpdateDTO request) {
        Tenant tenant = user.getTenant();
        tenant.setGeminiApiKey(request.getGeminiApiKey().trim());
        Tenant saved = tenantRepository.save(tenant);
        return ResponseEntity.ok(map(saved));
    }

    private TenantSettingsResponseDTO map(Tenant tenant) {
        String key = tenant.getGeminiApiKey();
        return TenantSettingsResponseDTO.builder()
                .tenantId(tenant.getId())
                .tenantName(tenant.getName())
                .geminiApiKeyConfigured(key != null && !key.isBlank())
                .geminiApiKeyMasked(mask(key))
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

