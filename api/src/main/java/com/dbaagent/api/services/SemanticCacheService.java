package com.dbaagent.api.services;

import com.dbaagent.api.entities.SemanticCache;
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

    public Optional<SemanticCache> checkCache(String schemaHash, Tenant tenant) {
        return repository.findBySchemaHashAndTenant(schemaHash, tenant);
    }

    public void saveToCache(String schemaHash, String improvement, String aiProvider, Tenant tenant) {
        SemanticCache cache = new SemanticCache(schemaHash, improvement, aiProvider);
        cache.setTenant(tenant); // Amarra o cache à empresa
        repository.save(cache);
    }
}