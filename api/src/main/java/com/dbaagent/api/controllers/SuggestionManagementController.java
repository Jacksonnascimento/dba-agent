package com.dbaagent.api.controllers;

import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.services.SuggestionManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/suggestions")
public class SuggestionManagementController {

    private final SuggestionManagementService service;

    public SuggestionManagementController(SuggestionManagementService service) {
        this.service = service;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<OptimizationSuggestion>> getPendingSuggestions(@AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(service.listPending(loggedUser.getTenant()));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<OptimizationSuggestion> approveSuggestion(@PathVariable Long id, @AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(service.approve(id, loggedUser.getTenant()));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<OptimizationSuggestion> rejectSuggestion(@PathVariable Long id, @AuthenticationPrincipal User loggedUser) {
        return ResponseEntity.ok(service.reject(id, loggedUser.getTenant()));
    }
}