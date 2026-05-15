package com.dbaagent.api.services;

import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.repositories.AgentTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import org.springframework.http.HttpStatus;

@Service
public class AgentTokenService {

    private final AgentTokenRepository repository;
    private final DatabaseConnectionRepository databaseConnectionRepository;

    public AgentTokenService(
            AgentTokenRepository repository,
            DatabaseConnectionRepository databaseConnectionRepository) {
        this.repository = repository;
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    public AgentToken generateTokenForTenant(String description, Long databaseConnectionId, Tenant tenant) {
        DatabaseConnection databaseConnection = databaseConnectionRepository
                .findByIdAndTenant(databaseConnectionId, tenant)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Banco não encontrado para o tenant informado."));

        String rawToken = UUID.randomUUID().toString();

        AgentToken agentToken = new AgentToken();
        agentToken.setTenant(tenant);
        agentToken.setDatabaseConnection(databaseConnection);
        agentToken.setToken(rawToken);
        agentToken.setDescription(description);
        agentToken.setActive(true);

        return repository.save(agentToken);
    }

    public java.util.List<com.dbaagent.api.dtos.AgentTokenResponseDTO> listByTenant(Tenant tenant) {
        return repository.findByTenantOrderByCreatedAtDesc(tenant).stream().map(token -> {
            String fullToken = token.getToken();
            String suffix = fullToken.length() > 4 ? "..." + fullToken.substring(fullToken.length() - 4) : fullToken;
            return com.dbaagent.api.dtos.AgentTokenResponseDTO.builder()
                    .id(token.getId())
                    .description(token.getDescription())
                    .databaseConnectionId(token.getDatabaseConnection().getId())
                    .databaseConnectionName(token.getDatabaseConnection().getName())
                    .tokenSuffix(suffix)
                    .createdAt(token.getCreatedAt())
                    .build();
        }).toList();
    }
}