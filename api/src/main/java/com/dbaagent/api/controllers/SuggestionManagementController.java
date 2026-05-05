package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.SuggestionResponseDTO;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.services.SuggestionManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionManagementController {

    private final SuggestionManagementService suggestionService;

    public SuggestionManagementController(SuggestionManagementService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<SuggestionResponseDTO>> getPendingSuggestions(@AuthenticationPrincipal User authenticatedUser) {
        List<SuggestionResponseDTO> pending = suggestionService.listPending(authenticatedUser.getTenant());
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SuggestionResponseDTO> approveSuggestion(
            @PathVariable Long id, 
            @AuthenticationPrincipal User authenticatedUser) {
        
        SuggestionResponseDTO approved = suggestionService.approve(id, authenticatedUser.getTenant());
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SuggestionResponseDTO> rejectSuggestion(
            @PathVariable Long id, 
            @AuthenticationPrincipal User authenticatedUser) {
        
        SuggestionResponseDTO rejected = suggestionService.reject(id, authenticatedUser.getTenant());
        return ResponseEntity.ok(rejected);
    }
}