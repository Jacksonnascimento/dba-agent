package com.dbaagent.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskResponseDTO {
    private Long id;
    private Long databaseConnectionId;
    private String databaseName;
    private String schemaHash;
    private String diagnosis;
    private String upScript;
    private String downScript;
    private String status;
}
