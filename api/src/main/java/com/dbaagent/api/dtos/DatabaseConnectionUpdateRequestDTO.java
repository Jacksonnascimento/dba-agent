package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DatabaseConnectionUpdateRequestDTO {

    @NotBlank(message = "Nome do banco é obrigatório")
    private String name;

    @NotBlank(message = "Engine é obrigatória")
    private String dbEngine;

    @NotNull(message = "Status ativo/inativo é obrigatório")
    private Boolean active;

    // Optional fields for URI update
    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;

    private String aiInstructionsAddon;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDbEngine() { return dbEngine; }
    public void setDbEngine(String dbEngine) { this.dbEngine = dbEngine; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAiInstructionsAddon() { return aiInstructionsAddon; }
    public void setAiInstructionsAddon(String aiInstructionsAddon) { this.aiInstructionsAddon = aiInstructionsAddon; }

}
