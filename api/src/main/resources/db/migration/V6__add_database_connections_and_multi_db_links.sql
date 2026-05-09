CREATE TABLE IF NOT EXISTS database_connections (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    db_engine VARCHAR(50) NOT NULL,
    connection_uri VARCHAR(2000) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_database_connections_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT uk_database_connections_tenant_name UNIQUE (tenant_id, name)
);

INSERT INTO database_connections (tenant_id, name, db_engine, connection_uri, is_active, created_at)
SELECT t.id,
       'default-' || t.id,
       'SQL Server',
       'jdbc:sqlserver://placeholder',
       TRUE,
       CURRENT_TIMESTAMP
FROM tenants t
WHERE NOT EXISTS (
    SELECT 1
    FROM database_connections dc
    WHERE dc.tenant_id = t.id
);

UPDATE semantic_cache
SET tenant_id = (SELECT MIN(id) FROM tenants)
WHERE tenant_id IS NULL;

UPDATE optimization_suggestions
SET tenant_id = (SELECT MIN(id) FROM tenants)
WHERE tenant_id IS NULL;

UPDATE agent_tokens
SET tenant_id = (SELECT MIN(id) FROM tenants)
WHERE tenant_id IS NULL;

ALTER TABLE agent_tokens ADD COLUMN IF NOT EXISTS database_connection_id BIGINT;
ALTER TABLE optimization_suggestions ADD COLUMN IF NOT EXISTS database_connection_id BIGINT;
ALTER TABLE semantic_cache ADD COLUMN IF NOT EXISTS database_connection_id BIGINT;

UPDATE agent_tokens at
SET database_connection_id = dc.id
FROM database_connections dc
WHERE at.database_connection_id IS NULL
  AND dc.tenant_id = at.tenant_id
  AND dc.id = (
      SELECT MIN(dc2.id)
      FROM database_connections dc2
      WHERE dc2.tenant_id = at.tenant_id
  );

UPDATE optimization_suggestions os
SET database_connection_id = dc.id
FROM database_connections dc
WHERE os.database_connection_id IS NULL
  AND dc.tenant_id = os.tenant_id
  AND dc.id = (
      SELECT MIN(dc2.id)
      FROM database_connections dc2
      WHERE dc2.tenant_id = os.tenant_id
  );

UPDATE semantic_cache sc
SET database_connection_id = dc.id
FROM database_connections dc
WHERE sc.database_connection_id IS NULL
  AND dc.tenant_id = sc.tenant_id
  AND dc.id = (
      SELECT MIN(dc2.id)
      FROM database_connections dc2
      WHERE dc2.tenant_id = sc.tenant_id
  );

ALTER TABLE agent_tokens
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE optimization_suggestions
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE semantic_cache
    ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE agent_tokens
    ALTER COLUMN database_connection_id SET NOT NULL;

ALTER TABLE optimization_suggestions
    ALTER COLUMN database_connection_id SET NOT NULL;

ALTER TABLE semantic_cache
    ALTER COLUMN database_connection_id SET NOT NULL;

ALTER TABLE agent_tokens
    ADD CONSTRAINT fk_agent_tokens_database_connection
    FOREIGN KEY (database_connection_id) REFERENCES database_connections(id);

ALTER TABLE optimization_suggestions
    ADD CONSTRAINT fk_optimization_suggestions_database_connection
    FOREIGN KEY (database_connection_id) REFERENCES database_connections(id);

ALTER TABLE semantic_cache
    ADD CONSTRAINT fk_semantic_cache_database_connection
    FOREIGN KEY (database_connection_id) REFERENCES database_connections(id);

ALTER TABLE semantic_cache DROP CONSTRAINT IF EXISTS semantic_cache_schema_hash_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_semantic_cache_tenant_db_hash
    ON semantic_cache (tenant_id, database_connection_id, schema_hash);
