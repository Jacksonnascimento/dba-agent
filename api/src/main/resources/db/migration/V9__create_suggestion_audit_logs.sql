CREATE TABLE suggestion_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    database_connection_id BIGINT NOT NULL,
    suggestion_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_type VARCHAR(30) NOT NULL,
    actor_identifier VARCHAR(255),
    details TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_audit_db_connection FOREIGN KEY (database_connection_id) REFERENCES database_connections(id),
    CONSTRAINT fk_audit_suggestion FOREIGN KEY (suggestion_id) REFERENCES optimization_suggestions(id)
);

CREATE INDEX idx_audit_tenant_db_created
    ON suggestion_audit_logs (tenant_id, database_connection_id, created_at DESC);
