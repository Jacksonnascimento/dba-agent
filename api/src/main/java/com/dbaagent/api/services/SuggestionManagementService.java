package com.dbaagent.api.services;

import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SuggestionManagementService {

    private final OptimizationSuggestionRepository repository;

    public SuggestionManagementService(OptimizationSuggestionRepository repository) {
        this.repository = repository;
    }

    public List<OptimizationSuggestion> listPending(Tenant tenant) {
        return repository.findByTenantAndStatus(tenant, SuggestionStatus.PENDING);
    }

    public OptimizationSuggestion approve(Long id, Tenant tenant) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        // Trava de Segurança Multi-Tenant
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }
        
        suggestion.setStatus(SuggestionStatus.APPROVED);
        return repository.save(suggestion);
    }

    public OptimizationSuggestion reject(Long id, Tenant tenant) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }

        suggestion.setStatus(SuggestionStatus.REJECTED);
        return repository.save(suggestion);
    }
}