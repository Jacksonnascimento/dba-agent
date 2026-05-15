package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class AgentWorkerUpdateRequestDTO {

    @NotBlank(message = "Nome do agente é obrigatório")
    private String name;

    @NotNull(message = "Intervalo é obrigatório")
    private Integer snapshotIntervalMinutes;

    private List<Long> databaseConnectionIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getSnapshotIntervalMinutes() { return snapshotIntervalMinutes; }
    public void setSnapshotIntervalMinutes(Integer snapshotIntervalMinutes) { this.snapshotIntervalMinutes = snapshotIntervalMinutes; }

    public List<Long> getDatabaseConnectionIds() { return databaseConnectionIds; }
    public void setDatabaseConnectionIds(List<Long> databaseConnectionIds) { this.databaseConnectionIds = databaseConnectionIds; }
}
