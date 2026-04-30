package com.dbaagent.api.services;

import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.AgentTokenRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgentTokenService {

    private final AgentTokenRepository repository;

    public AgentTokenService(AgentTokenRepository repository) {
        this.repository = repository;
    }

    public AgentToken generateTokenForTenant(String description, Tenant tenant) {
        // Gera um Token UUID v4 (padrão de mercado para chaves de API/Agentes)
        String rawToken = UUID.randomUUID().toString();

        AgentToken agentToken = new AgentToken();
        agentToken.setTenant(tenant);
        agentToken.setToken(rawToken);
        agentToken.setDescription(description);
        agentToken.setActive(true);

        return repository.save(agentToken);
    }
}