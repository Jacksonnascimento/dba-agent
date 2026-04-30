package com.dbaagent.api.dtos;

public class OptimizationResponseDTO {

    private Long suggestionId; // O ID para o front-end aprovar depois
    private String suggestedImprovement; // O Diagnóstico
    private String upScript;
    private String downScript;
    private boolean fromCache;
    private String aiProvider;

    public OptimizationResponseDTO() {}

    // Construtor completo (Para o sucesso com a IA)
    public OptimizationResponseDTO(Long suggestionId, String suggestedImprovement, String upScript, String downScript, boolean fromCache, String aiProvider) {
        this.suggestionId = suggestionId;
        this.suggestedImprovement = suggestedImprovement;
        this.upScript = upScript;
        this.downScript = downScript;
        this.fromCache = fromCache;
        this.aiProvider = aiProvider;
    }

    // Construtor simples (Para o Linter ou erros)
    public OptimizationResponseDTO(String suggestedImprovement, boolean fromCache, String aiProvider) {
        this.suggestedImprovement = suggestedImprovement;
        this.fromCache = fromCache;
        this.aiProvider = aiProvider;
    }

    // Getters e Setters
    public Long getSuggestionId() { return suggestionId; }
    public void setSuggestionId(Long suggestionId) { this.suggestionId = suggestionId; }

    public String getSuggestedImprovement() { return suggestedImprovement; }
    public void setSuggestedImprovement(String suggestedImprovement) { this.suggestedImprovement = suggestedImprovement; }

    public String getUpScript() { return upScript; }
    public void setUpScript(String upScript) { this.upScript = upScript; }

    public String getDownScript() { return downScript; }
    public void setDownScript(String downScript) { this.downScript = downScript; }

    public boolean isFromCache() { return fromCache; }
    public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }

    public String getAiProvider() { return aiProvider; }
    public void setAiProvider(String aiProvider) { this.aiProvider = aiProvider; }
}