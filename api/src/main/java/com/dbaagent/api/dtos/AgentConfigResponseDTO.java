package com.dbaagent.api.dtos;

public class AgentConfigResponseDTO {
    private String name;
    private String dbEngine;
    private String connectionUri;
    private Integer snapshotIntervalMinutes;

    public AgentConfigResponseDTO() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDbEngine() { return dbEngine; }
    public void setDbEngine(String dbEngine) { this.dbEngine = dbEngine; }

    public String getConnectionUri() { return connectionUri; }
    public void setConnectionUri(String connectionUri) { this.connectionUri = connectionUri; }

    public Integer getSnapshotIntervalMinutes() { return snapshotIntervalMinutes; }
    public void setSnapshotIntervalMinutes(Integer snapshotIntervalMinutes) { this.snapshotIntervalMinutes = snapshotIntervalMinutes; }
}