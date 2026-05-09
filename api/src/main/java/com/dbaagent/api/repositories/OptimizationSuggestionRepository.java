package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptimizationSuggestionRepository extends JpaRepository<OptimizationSuggestion, Long> {
    
    List<OptimizationSuggestion> findByTenantAndStatus(Tenant tenant, SuggestionStatus status);
    List<OptimizationSuggestion> findByTenantAndDatabaseConnectionAndStatus(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            SuggestionStatus status);

    Optional<OptimizationSuggestion> findByTenantAndDatabaseConnectionAndSchemaHashAndStatus(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            String schemaHash,
            SuggestionStatus status);

    Optional<OptimizationSuggestion> findByTenantAndDatabaseConnectionAndContextHashAndStatus(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            String contextHash,
            SuggestionStatus status);
}