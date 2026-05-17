package com.dbaagent.api.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "agent_workers")
public class AgentWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "worker_token", nullable = false, unique = true, length = 100)
    private String workerToken;

    @Column(name = "snapshot_interval_minutes", nullable = false)
    private Integer snapshotIntervalMinutes = 1440; // Default: 24h

    @Column(name = "ai_instructions_addon", columnDefinition = "TEXT")
    private String aiInstructionsAddon;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "agent_worker_databases",
            joinColumns = @JoinColumn(name = "agent_worker_id"),
            inverseJoinColumns = @JoinColumn(name = "database_connection_id")
    )
    private Set<DatabaseConnection> databases = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void generateToken() {
        if (this.workerToken == null) {
            this.workerToken = UUID.randomUUID().toString();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWorkerToken() { return workerToken; }
    public void setWorkerToken(String workerToken) { this.workerToken = workerToken; }

    public Integer getSnapshotIntervalMinutes() { return snapshotIntervalMinutes; }
    public void setSnapshotIntervalMinutes(Integer snapshotIntervalMinutes) { this.snapshotIntervalMinutes = snapshotIntervalMinutes; }

    public String getAiInstructionsAddon() { return aiInstructionsAddon; }
    public void setAiInstructionsAddon(String aiInstructionsAddon) { this.aiInstructionsAddon = aiInstructionsAddon; }

    public Set<DatabaseConnection> getDatabases() { return databases; }
    public void setDatabases(Set<DatabaseConnection> databases) { this.databases = databases; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
