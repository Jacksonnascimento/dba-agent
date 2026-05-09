package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class AgentTelemetryRequestDTO {

    @NotBlank(message = "O DDL do banco (schema) é obrigatório para análise")
    private String schemaDdl;

    private String dmvStats;
    private String dbEngine = "SQL Server";

    // Contexto avançado (opcional): quanto mais, melhor a precisão.
    private String waitStats;
    private String topQueries;
    private String executionPlans;
    private String indexStats;

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

    public String getWaitStats() {
        return waitStats;
    }

    public void setWaitStats(String waitStats) {
        this.waitStats = waitStats;
    }

    public String getTopQueries() {
        return topQueries;
    }

    public void setTopQueries(String topQueries) {
        this.topQueries = topQueries;
    }

    public String getExecutionPlans() {
        return executionPlans;
    }

    public void setExecutionPlans(String executionPlans) {
        this.executionPlans = executionPlans;
    }

    public String getIndexStats() {
        return indexStats;
    }

    public void setIndexStats(String indexStats) {
        this.indexStats = indexStats;
    }
}