ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS database_name VARCHAR(255) NOT NULL DEFAULT 'default_db';

ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS table_name VARCHAR(255);

ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS suggestion_text TEXT NOT NULL DEFAULT 'Sugestão gerada pelo motor';

ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS up_script TEXT NOT NULL DEFAULT '-- script de deploy';

ALTER TABLE optimization_suggestions
    ADD COLUMN IF NOT EXISTS down_script TEXT NOT NULL DEFAULT '-- script de rollback';