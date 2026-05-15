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
    public ResponseEntity<List<SuggestionResponseDTO>> getPendingSuggestions(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(required = false) Long databaseConnectionId) {
        List<SuggestionResponseDTO> pending =
                suggestionService.listPending(authenticatedUser.getTenant(), databaseConnectionId);
        return ResponseEntity.ok(pending);
    }

    @GetMapping("/executed")
    public ResponseEntity<List<SuggestionResponseDTO>> getExecutedSuggestions(
            @AuthenticationPrincipal User authenticatedUser,
            @RequestParam(required = false) Long databaseConnectionId) {
        List<SuggestionResponseDTO> executed =
                suggestionService.listExecuted(authenticatedUser.getTenant(), databaseConnectionId);
        return ResponseEntity.ok(executed);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<SuggestionResponseDTO> approveSuggestion(
            @PathVariable Long id, 
            @AuthenticationPrincipal User authenticatedUser) {
        
        SuggestionResponseDTO approved = suggestionService.approve(id, authenticatedUser.getTenant(), authenticatedUser);
        return ResponseEntity.ok(approved);
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<SuggestionResponseDTO> rejectSuggestion(
            @PathVariable Long id, 
            @AuthenticationPrincipal User authenticatedUser) {
        
        SuggestionResponseDTO rejected = suggestionService.reject(id, authenticatedUser.getTenant(), authenticatedUser);
        return ResponseEntity.ok(rejected);
    }

    @PostMapping("/{id}/request-rollback")
    public ResponseEntity<SuggestionResponseDTO> requestRollback(
            @PathVariable Long id, 
            @AuthenticationPrincipal User authenticatedUser) {
        
        SuggestionResponseDTO rollback = suggestionService.requestRollback(id, authenticatedUser.getTenant(), authenticatedUser);
        return ResponseEntity.ok(rollback);
    }
}