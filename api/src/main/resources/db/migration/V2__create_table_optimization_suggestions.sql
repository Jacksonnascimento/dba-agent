CREATE TABLE optimization_suggestions (
    id BIGSERIAL PRIMARY KEY,
    schema_hash VARCHAR(255) NOT NULL,
    diagnosis TEXT NOT NULL,
    up_script TEXT NOT NULL,
    down_script TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMP
);