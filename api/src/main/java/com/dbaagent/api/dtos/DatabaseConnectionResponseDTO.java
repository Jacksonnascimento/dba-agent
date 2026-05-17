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
public class DatabaseConnectionResponseDTO {
    private Long id;
    private String name;
    private String dbEngine;
    private Boolean active;
    private String aiInstructionsAddon;
    private LocalDateTime createdAt;
}
