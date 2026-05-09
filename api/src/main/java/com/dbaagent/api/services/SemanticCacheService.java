package com.dbaagent.api.services;

import com.dbaagent.api.entities.SemanticCache;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.SemanticCacheRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SemanticCacheService {

    private final SemanticCacheRepository repository;

    public SemanticCacheService(SemanticCacheRepository repository) {
        this.repository = repository;
    }

    public Optional<SemanticCache> checkCache(
            String schemaHash,
            Tenant tenant,
            DatabaseConnection databaseConnection) {
        return repository.findBySchemaHashAndTenantAndDatabaseConnection(schemaHash, tenant, databaseConnection);
    }

    public Optional<SemanticCache> checkCacheByContextHash(
            String contextHash,
            Tenant tenant,
            DatabaseConnection databaseConnection) {
        return repository.findByContextHashAndTenantAndDatabaseConnection(contextHash, tenant, databaseConnection);
    }

    public void saveToCache(
            String schemaHash,
            String contextHash,
            String improvement,
            String aiProvider,
            Tenant tenant,
            DatabaseConnection databaseConnection) {
        SemanticCache cache = new SemanticCache(schemaHash, improvement, aiProvider);
        cache.setContextHash(contextHash);
        cache.setTenant(tenant);
        cache.setDatabaseConnection(databaseConnection);
        repository.save(cache);
    }
}