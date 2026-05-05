package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptimizationSuggestionRepository extends JpaRepository<OptimizationSuggestion, Long> {
    
    List<OptimizationSuggestion> findByTenantAndStatus(Tenant tenant, SuggestionStatus status);
    
}