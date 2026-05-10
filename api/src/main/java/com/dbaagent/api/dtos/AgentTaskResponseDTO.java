package com.dbaagent.api.dtos;

public class AgentTaskResponseDTO {
    private Long id;
    private Long databaseConnectionId;
    private String databaseName;
    private String schemaHash;
    private String diagnosis;
    private String upScript;
    private String downScript;
    private String status;

    public AgentTaskResponseDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDatabaseConnectionId() { return databaseConnectionId; }
    public void setDatabaseConnectionId(Long databaseConnectionId) { this.databaseConnectionId = databaseConnectionId; }

    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }

    public String getSchemaHash() { return schemaHash; }
    public void setSchemaHash(String schemaHash) { this.schemaHash = schemaHash; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getUpScript() { return upScript; }
    public void setUpScript(String upScript) { this.upScript = upScript; }

    public String getDownScript() { return downScript; }
    public void setDownScript(String downScript) { this.downScript = downScript; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Implementação nativa do Builder exigida pela sua Controller
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AgentTaskResponseDTO dto = new AgentTaskResponseDTO();

        public Builder id(Long id) { dto.setId(id); return this; }
        public Builder databaseConnectionId(Long databaseConnectionId) { dto.setDatabaseConnectionId(databaseConnectionId); return this; }
        public Builder databaseName(String databaseName) { dto.setDatabaseName(databaseName); return this; }
        public Builder schemaHash(String schemaHash) { dto.setSchemaHash(schemaHash); return this; }
        public Builder diagnosis(String diagnosis) { dto.setDiagnosis(diagnosis); return this; }
        public Builder upScript(String upScript) { dto.setUpScript(upScript); return this; }
        public Builder downScript(String downScript) { dto.setDownScript(downScript); return this; }
        public Builder status(String status) { dto.setStatus(status); return this; }
        
        public AgentTaskResponseDTO build() { return dto; }
    }
}