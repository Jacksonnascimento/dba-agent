-- 1. Criação da Tabela de Tenants (Empresas/Clientes)
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    gemini_api_key VARCHAR(500), -- Cofre para a chave do cliente (BYOK)
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Criação da Tabela de Usuários (Acesso Web / Humanos)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_CLIENT', -- Perfis: ROLE_ADMIN, ROLE_CLIENT
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- 3. Criação da Tabela de Tokens do Agente (Acesso Máquina / DBAs de Ferro)
CREATE TABLE agent_tokens (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255), -- Ex: "Servidor SQL Server Prod Principal"
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    CONSTRAINT fk_agents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- 4. Alterando as tabelas existentes para pertencerem a um Tenant
ALTER TABLE semantic_cache ADD COLUMN tenant_id BIGINT;
ALTER TABLE semantic_cache ADD CONSTRAINT fk_cache_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);

ALTER TABLE optimization_suggestions ADD COLUMN tenant_id BIGINT;
ALTER TABLE optimization_suggestions ADD CONSTRAINT fk_suggestions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id);