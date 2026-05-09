package com.dbaagent.api.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantSettingsResponseDTO {
    private Long tenantId;
    private String tenantName;
    private Boolean geminiApiKeyConfigured;
    private String geminiApiKeyMasked;
}

