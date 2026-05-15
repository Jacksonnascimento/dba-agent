-- Adiciona os campos para suportar o provedor Claude e seleção de modelo

ALTER TABLE tenants ADD COLUMN ai_provider VARCHAR(50) DEFAULT 'GEMINI';
ALTER TABLE tenants ADD COLUMN ai_model VARCHAR(100);
ALTER TABLE tenants ADD COLUMN claude_api_key VARCHAR(500);

-- Atualiza tenants existentes para ter o provider GEMINI
UPDATE tenants SET ai_provider = 'GEMINI' WHERE ai_provider IS NULL;
