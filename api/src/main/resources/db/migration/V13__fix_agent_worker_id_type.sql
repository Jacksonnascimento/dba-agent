-- V13__fix_agent_worker_id_type.sql

-- O JPA espera um Long (BIGINT), mas criamos com SERIAL (INTEGER) na V12.
ALTER TABLE agent_workers ALTER COLUMN id TYPE BIGINT;
