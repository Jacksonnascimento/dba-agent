package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentTelemetryRequestDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.DatabaseTelemetrySnapshot;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.services.DatabaseTelemetrySnapshotService;
import com.dbaagent.api.services.OptimizationAnalysisService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/agent/telemetry")
public class AgentTelemetryController {

    private final OptimizationAnalysisService optimizationAnalysisService;
    private final DatabaseTelemetrySnapshotService snapshotService;

    public AgentTelemetryController(
            OptimizationAnalysisService optimizationAnalysisService,
            DatabaseTelemetrySnapshotService snapshotService) {
        this.optimizationAnalysisService = optimizationAnalysisService;
        this.snapshotService = snapshotService;
    }

    @PostMapping
    public ResponseEntity<String> receiveTelemetry(@Valid @RequestBody AgentTelemetryRequestDTO telemetry) {
        AgentToken agentToken = requireAgentToken();
        Tenant tenant = agentToken.getTenant();

        DatabaseTelemetrySnapshot snapshot = snapshotService.persistSnapshot(
                tenant,
                agentToken.getDatabaseConnection(),
                telemetry);

        OptimizationSuggestion saved = optimizationAnalysisService.analyzeAndPersistFromSnapshot(
                tenant,
                agentToken.getDatabaseConnection(),
                snapshot.getSchemaDdl(),
                snapshot.getDmvStats(),
                snapshot.getWaitStats(),
                snapshot.getTopQueries(),
                snapshot.getExecutionPlans(),
                snapshot.getIndexStats(),
                snapshot.getDbEngine(),
                null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Telemetria processada. Sugestão id=" + saved.getId() + " status=PENDING.");
    }

    private static AgentToken requireAgentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AgentToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agente não autenticado.");
        }
        return (AgentToken) auth.getPrincipal();
    }
}