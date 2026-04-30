CREATE TABLE semantic_cache (
    id BIGSERIAL PRIMARY KEY,
    schema_hash VARCHAR(255) NOT NULL UNIQUE,
    suggested_improvement TEXT NOT NULL,
    ai_provider VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL
);