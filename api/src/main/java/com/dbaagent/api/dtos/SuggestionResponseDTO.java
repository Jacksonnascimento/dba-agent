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
public class SuggestionResponseDTO {
    private Long id;
    private Long databaseConnectionId;
    private String databaseConnectionName;
    private String databaseName;
    private String tableName;
    private String suggestionText;
    private String upScript;
    private String downScript;
    private String status;
    private LocalDateTime createdAt;
}