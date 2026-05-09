package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.SuggestionAuditLogResponseDTO;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.services.SuggestionAuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit/suggestions")
public class SuggestionAuditLogController {

    private final SuggestionAuditLogService service;

    public SuggestionAuditLogController(SuggestionAuditLogService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<SuggestionAuditLogResponseDTO>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long databaseConnectionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int bounded = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(service.list(user.getTenant(), databaseConnectionId, PageRequest.of(page, bounded)));
    }
}

