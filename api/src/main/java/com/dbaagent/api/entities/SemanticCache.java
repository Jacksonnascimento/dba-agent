package com.dbaagent.api.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "semantic_cache")
public class SemanticCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_hash", nullable = false, unique = true, length = 255)
    private String schemaHash; // Hash da estrutura DDL para busca rápida

    @Column(name = "suggested_improvement", columnDefinition = "TEXT", nullable = false)
    private String suggestedImprovement; // O script/sugestão gerada pela IA

    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider; // Gemini ou Claude

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // Construtor vazio exigido pelo JPA
    public SemanticCache() {
    }

    public SemanticCache(String schemaHash, String suggestedImprovement, String aiProvider) {
        this.schemaHash = schemaHash;
        this.suggestedImprovement = suggestedImprovement;
        this.aiProvider = aiProvider;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
    }

    public String getSuggestedImprovement() {
        return suggestedImprovement;
    }

    public void setSuggestedImprovement(String suggestedImprovement) {
        this.suggestedImprovement = suggestedImprovement;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
}