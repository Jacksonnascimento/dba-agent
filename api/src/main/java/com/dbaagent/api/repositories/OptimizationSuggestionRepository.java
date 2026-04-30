package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OptimizationSuggestionRepository extends JpaRepository<OptimizationSuggestion, Long> {

    Optional<OptimizationSuggestion> findBySchemaHashAndStatus(String schemaHash, SuggestionStatus status);

    List<OptimizationSuggestion> findByStatus(SuggestionStatus status);

    // Método seguro que busca filtrando pela Empresa
    List<OptimizationSuggestion> findByTenantAndStatus(Tenant tenant, SuggestionStatus status);
}