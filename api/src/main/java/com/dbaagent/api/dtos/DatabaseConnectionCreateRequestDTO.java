package com.dbaagent.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DatabaseConnectionCreateRequestDTO {

    @NotBlank(message = "Nome do banco é obrigatório")
    private String name;

    @NotBlank(message = "Engine é obrigatória")
    private String dbEngine;

    @NotBlank(message = "Host/IP é obrigatório")
    private String host;

    @NotNull(message = "Porta é obrigatória")
    private Integer port;

    @NotBlank(message = "Nome do database é obrigatório")
    private String database;

    @NotBlank(message = "Usuário é obrigatório")
    private String username;

    @NotBlank(message = "Senha é obrigatória")
    private String password;

    private String aiInstructionsAddon;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDbEngine() { return dbEngine; }
    public void setDbEngine(String dbEngine) { this.dbEngine = dbEngine; }

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