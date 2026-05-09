package com.dbaagent.api.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SuggestionAuditLogResponseDTO {
    private Long id;
    private Long suggestionId;
    private Long databaseConnectionId;
    private String databaseConnectionName;
    private String action;
    private String actorType;
    private String actorIdentifier;
    private String details;
    private LocalDateTime createdAt;
}

