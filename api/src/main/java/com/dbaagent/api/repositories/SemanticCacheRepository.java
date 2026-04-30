package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.SemanticCache;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemanticCacheRepository extends JpaRepository<SemanticCache, Long> {
    
    // Agora o cache busca pelo Hash E pela Empresa logada
    Optional<SemanticCache> findBySchemaHashAndTenant(String schemaHash, Tenant tenant);
}