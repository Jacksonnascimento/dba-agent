CREATE TABLE database_telemetry_snapshots (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    database_connection_id BIGINT NOT NULL,
    schema_hash VARCHAR(255) NOT NULL,
    context_hash VARCHAR(255) NOT NULL,
    db_engine VARCHAR(50) NOT NULL,
    schema_ddl TEXT NOT NULL,
    dmv_stats TEXT,
    wait_stats TEXT,
    top_queries TEXT,
    execution_plans TEXT,
    index_stats TEXT,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_snapshots_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_snapshots_database_connection FOREIGN KEY (database_connection_id) REFERENCES database_connections(id)
);

CREATE INDEX idx_snapshots_tenant_db_collected_at
    ON database_telemetry_snapshots (tenant_id, database_connection_id, collected_at DESC);
