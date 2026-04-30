package com.dbaagent.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiAnalysisResultDTO {

    @JsonProperty("diagnostico")
    private String diagnostico;

    @JsonProperty("up_script")
    private String upScript;

    @JsonProperty("down_script")
    private String downScript;

    // Getters e Setters
    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getUpScript() { return upScript; }
    public void setUpScript(String upScript) { this.upScript = upScript; }

    public String getDownScript() { return downScript; }
    public void setDownScript(String downScript) { this.downScript = downScript; }
}