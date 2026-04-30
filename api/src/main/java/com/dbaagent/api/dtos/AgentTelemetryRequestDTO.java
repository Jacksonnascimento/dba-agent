package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class AgentTelemetryRequestDTO {

    @NotBlank(message = "O DDL do banco (schema) é obrigatório para análise")
    private String schemaDdl;

    private String dmvStats;
    private String dbEngine = "SQL Server";

    public AgentTelemetryRequestDTO() {}

    // Getters e Setters
    public String getSchemaDdl() { return schemaDdl; }
    public void setSchemaDdl(String schemaDdl) { this.schemaDdl = schemaDdl; }

    public String getDmvStats() { return dmvStats; }
    public void setDmvStats(String dmvStats) { this.dmvStats = dmvStats; }

    public String getDbEngine() { return dbEngine; }
    public void setDbEngine(String dbEngine) {
        if (dbEngine != null && !dbEngine.trim().isEmpty()) {
            this.dbEngine = dbEngine;
        }
    }
}