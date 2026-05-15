-- V12__create_agent_worker_tables.sql

CREATE TABLE agent_workers (
    id SERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    worker_token VARCHAR(100) NOT NULL UNIQUE,
    snapshot_interval_minutes INTEGER NOT NULL DEFAULT 1440,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT fk_agent_workers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE TABLE agent_worker_databases (
    agent_worker_id BIGINT NOT NULL,
    database_connection_id BIGINT NOT NULL,
    
    PRIMARY KEY (agent_worker_id, database_connection_id),
    CONSTRAINT fk_awd_worker FOREIGN KEY (agent_worker_id) REFERENCES agent_workers(id) ON DELETE CASCADE,
    CONSTRAINT fk_awd_database FOREIGN KEY (database_connection_id) REFERENCES database_connections(id) ON DELETE CASCADE
);
