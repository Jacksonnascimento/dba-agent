package com.dbaagent.api.services;

import com.dbaagent.api.dtos.SuggestionResponseDTO;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SuggestionManagementService {

    private final OptimizationSuggestionRepository repository;

    public SuggestionManagementService(OptimizationSuggestionRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SuggestionResponseDTO> listPending(Tenant tenant) {
        List<OptimizationSuggestion> suggestions = repository.findByTenantAndStatus(tenant, SuggestionStatus.PENDING);
        
        return suggestions.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public SuggestionResponseDTO approve(Long id, Tenant tenant) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        // Trava de Segurança Multi-Tenant
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }
        
        // Na nossa arquitetura BYOK/Worker (ADR), o Agente faz Polling na API.
        // Ao atualizar para APPROVED, o próximo ciclo do Agente fará o pull deste upScript e o executará.
        suggestion.setStatus(SuggestionStatus.APPROVED);
        repository.save(suggestion);
        
        return mapToDTO(suggestion);
    }

    @Transactional
    public SuggestionResponseDTO reject(Long id, Tenant tenant) {
        OptimizationSuggestion suggestion = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sugestão não encontrada com o ID: " + id));
        
        if (!suggestion.getTenant().getId().equals(tenant.getId())) {
            throw new RuntimeException("Acesso negado: Esta sugestão pertence a outra empresa.");
        }

        suggestion.setStatus(SuggestionStatus.REJECTED);
        repository.save(suggestion);
        
        return mapToDTO(suggestion);
    }

    private SuggestionResponseDTO mapToDTO(OptimizationSuggestion suggestion) {
        return SuggestionResponseDTO.builder()
                .id(suggestion.getId())
                .databaseName(suggestion.getDatabaseName())
                .tableName(suggestion.getTableName())
                .suggestionText(suggestion.getSuggestionText())
                .upScript(suggestion.getUpScript())
                .downScript(suggestion.getDownScript())
                .status(suggestion.getStatus().name())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}