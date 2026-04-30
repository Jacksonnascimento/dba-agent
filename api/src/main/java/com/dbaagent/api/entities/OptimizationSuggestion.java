package com.dbaagent.api.entities;

import com.dbaagent.api.enums.SuggestionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "optimization_suggestions")
public class OptimizationSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "schema_hash", nullable = false)
    private String schemaHash;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String diagnosis;

    @Column(name = "up_script", columnDefinition = "TEXT", nullable = false)
    private String upScript;

    @Column(name = "down_script", columnDefinition = "TEXT", nullable = false)
    private String downScript;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionStatus status = SuggestionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    // Construtor vazio obrigatório do JPA
    public OptimizationSuggestion() {}

    public OptimizationSuggestion(String schemaHash, String diagnosis, String upScript, String downScript) {
        this.schemaHash = schemaHash;
        this.diagnosis = diagnosis;
        this.upScript = upScript;
        this.downScript = downScript;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSchemaHash() { return schemaHash; }
    public void setSchemaHash(String schemaHash) { this.schemaHash = schemaHash; }

    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }

    public String getUpScript() { return upScript; }
    public void setUpScript(String upScript) { this.upScript = upScript; }

    public String getDownScript() { return downScript; }
    public void setDownScript(String downScript) { this.downScript = downScript; }

    public SuggestionStatus getStatus() { return status; }
    public void setStatus(SuggestionStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
}