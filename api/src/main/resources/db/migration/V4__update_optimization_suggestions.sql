ALTER TABLE optimization_suggestions
ADD COLUMN database_name VARCHAR(255) NOT NULL DEFAULT 'default_db',
ADD COLUMN table_name VARCHAR(255),
ADD COLUMN suggestion_text TEXT NOT NULL DEFAULT 'Sugestão gerada pelo motor',
ADD COLUMN up_script TEXT NOT NULL DEFAULT '-- script de deploy',
ADD COLUMN down_script TEXT NOT NULL DEFAULT '-- script de rollback';