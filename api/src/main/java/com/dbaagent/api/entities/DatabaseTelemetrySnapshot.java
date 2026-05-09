package com.dbaagent.api.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "database_telemetry_snapshots",
        indexes = {
                @Index(name = "idx_snapshots_tenant_db_collected_at", columnList = "tenant_id,database_connection_id,collected_at")
        }
)
public class DatabaseTelemetrySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_connection_id", nullable = false)
    private DatabaseConnection databaseConnection;

    @Column(name = "schema_hash", nullable = false, length = 255)
    private String schemaHash;

    @Column(name = "context_hash", nullable = false, length = 255)
    private String contextHash;

    @Column(name = "db_engine", nullable = false, length = 50)
    private String dbEngine;

    @Column(name = "schema_ddl", columnDefinition = "TEXT", nullable = false)
    private String schemaDdl;

    @Column(name = "dmv_stats", columnDefinition = "TEXT")
    private String dmvStats;

    @Column(name = "wait_stats", columnDefinition = "TEXT")
    private String waitStats;

    @Column(name = "top_queries", columnDefinition = "TEXT")
    private String topQueries;

    @Column(name = "execution_plans", columnDefinition = "TEXT")
    private String executionPlans;

    @Column(name = "index_stats", columnDefinition = "TEXT")
    private String indexStats;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    @PrePersist
    protected void onCreate() {
        this.collectedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public void setDatabaseConnection(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
    }

    public String getContextHash() {
        return contextHash;
    }

    public void setContextHash(String contextHash) {
        this.contextHash = contextHash;
    }

    public String getDbEngine() {
        return dbEngine;
    }

    public void setDbEngine(String dbEngine) {
        this.dbEngine = dbEngine;
    }

    public String getSchemaDdl() {
        return schemaDdl;
    }

    public void setSchemaDdl(String schemaDdl) {
        this.schemaDdl = schemaDdl;
    }

    public String getDmvStats() {
        return dmvStats;
    }

    public void setDmvStats(String dmvStats) {
        this.dmvStats = dmvStats;
    }

    public String getWaitStats() {
        return waitStats;
    }

    public void setWaitStats(String waitStats) {
        this.waitStats = waitStats;
    }

    public String getTopQueries() {
        return topQueries;
    }

    public void setTopQueries(String topQueries) {
        this.topQueries = topQueries;
    }

    public String getExecutionPlans() {
        return executionPlans;
    }

    public void setExecutionPlans(String executionPlans) {
        this.executionPlans = executionPlans;
    }

    public String getIndexStats() {
        return indexStats;
    }

    public void setIndexStats(String indexStats) {
        this.indexStats = indexStats;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
}

