package com.dbaagent.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTokenResponseDTO {
    private Long id;
    private String description;
    private Long databaseConnectionId;
    private String databaseConnectionName;
    private String tokenSuffix; // Somente o final do token por segurança
    private LocalDateTime createdAt;
}
