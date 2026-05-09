package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentTokenRequestDTO {

    @NotBlank(message = "A descrição do agente é obrigatória")
    private String description;

    @NotNull(message = "databaseConnectionId é obrigatório")
    private Long databaseConnectionId;

    public AgentTokenRequestDTO() {}

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getDatabaseConnectionId() {
        return databaseConnectionId;
    }

    public void setDatabaseConnectionId(Long databaseConnectionId) {
        this.databaseConnectionId = databaseConnectionId;
    }
}