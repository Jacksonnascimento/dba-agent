package com.dbaagent.api.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suggestion_audit_logs", indexes = {
        @Index(name = "idx_audit_tenant_db_created", columnList = "tenant_id,database_connection_id,created_at")
})
public class SuggestionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_connection_id", nullable = false)
    private DatabaseConnection databaseConnection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suggestion_id", nullable = false)
    private OptimizationSuggestion suggestion;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "actor_type", nullable = false, length = 30)
    private String actorType;

    @Column(name = "actor_identifier", length = 255)
    private String actorIdentifier;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public DatabaseConnection getDatabaseConnection() { return databaseConnection; }
    public void setDatabaseConnection(DatabaseConnection databaseConnection) { this.databaseConnection = databaseConnection; }
    public OptimizationSuggestion getSuggestion() { return suggestion; }
    public void setSuggestion(OptimizationSuggestion suggestion) { this.suggestion = suggestion; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getActorIdentifier() { return actorIdentifier; }
    public void setActorIdentifier(String actorIdentifier) { this.actorIdentifier = actorIdentifier; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

