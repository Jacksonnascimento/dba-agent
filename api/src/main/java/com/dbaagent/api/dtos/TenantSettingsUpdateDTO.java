package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class TenantSettingsUpdateDTO {

    @NotBlank(message = "geminiApiKey é obrigatória")
    private String geminiApiKey;

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }
}

