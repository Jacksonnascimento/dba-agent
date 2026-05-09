ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS context_hash VARCHAR(255);

ALTER TABLE semantic_cache
    ADD COLUMN IF NOT EXISTS context_hash VARCHAR(255);

UPDATE optimization_suggestions
SET context_hash = schema_hash
WHERE context_hash IS NULL;

UPDATE semantic_cache
SET context_hash = schema_hash
WHERE context_hash IS NULL;

ALTER TABLE optimization_suggestions
    ALTER COLUMN context_hash SET NOT NULL;

ALTER TABLE semantic_cache
    ALTER COLUMN context_hash SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_suggestions_tenant_db_context_status
    ON optimization_suggestions (tenant_id, database_connection_id, context_hash, status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_semantic_cache_tenant_db_context
    ON semantic_cache (tenant_id, database_connection_id, context_hash);
