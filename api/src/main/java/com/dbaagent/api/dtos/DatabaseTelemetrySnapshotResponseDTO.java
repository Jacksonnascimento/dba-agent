package com.dbaagent.api.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DatabaseTelemetrySnapshotResponseDTO {
    private Long id;
    private String schemaHash;
    private String contextHash;
    private String dbEngine;
    private String dmvStats;
    private String waitStats;
    private String topQueries;
    private String executionPlans;
    private String indexStats;
    private LocalDateTime collectedAt;
}

