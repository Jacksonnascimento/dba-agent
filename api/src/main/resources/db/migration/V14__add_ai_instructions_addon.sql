ALTER TABLE agent_workers
    ADD COLUMN IF NOT EXISTS ai_instructions_addon TEXT;

ALTER TABLE database_connections
    ADD COLUMN IF NOT EXISTS ai_instructions_addon TEXT;
