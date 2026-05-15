package com.dbaagent.api.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantSettingsResponseDTO {
    private Long tenantId;
    private String tenantName;
    private String aiProvider;
    private String aiModel;
    private Boolean geminiApiKeyConfigured;
    private String geminiApiKeyMasked;
    private Boolean claudeApiKeyConfigured;
    private String claudeApiKeyMasked;
}

