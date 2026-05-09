package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;

public class DatabaseConnectionCreateRequestDTO {

    @NotBlank(message = "Nome do banco é obrigatório")
    private String name;

    @NotBlank(message = "Engine é obrigatória")
    private String dbEngine;

    @NotBlank(message = "Connection URI é obrigatória")
    private String connectionUri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDbEngine() {
        return dbEngine;
    }

    public void setDbEngine(String dbEngine) {
        this.dbEngine = dbEngine;
    }

    public String getConnectionUri() {
        return connectionUri;
    }

    public void setConnectionUri(String connectionUri) {
        this.connectionUri = connectionUri;
    }
}
