ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS applied_at TIMESTAMP;