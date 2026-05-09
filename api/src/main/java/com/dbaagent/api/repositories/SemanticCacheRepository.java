package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.SemanticCache;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SemanticCacheRepository extends JpaRepository<SemanticCache, Long> {
    
    Optional<SemanticCache> findBySchemaHashAndTenantAndDatabaseConnection(
            String schemaHash,
            Tenant tenant,
            DatabaseConnection databaseConnection);

    Optional<SemanticCache> findByContextHashAndTenantAndDatabaseConnection(
            String contextHash,
            Tenant tenant,
            DatabaseConnection databaseConnection);
}