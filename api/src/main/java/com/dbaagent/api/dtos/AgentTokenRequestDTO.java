package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class AgentTokenRequestDTO {

    @NotBlank(message = "A descrição do agente é obrigatória")
    private String description;

    public AgentTokenRequestDTO() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}