package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentWorkerCreateRequestDTO;
import com.dbaagent.api.dtos.AgentWorkerResponseDTO;
import com.dbaagent.api.dtos.AgentWorkerUpdateRequestDTO;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.services.AgentWorkerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent-workers")
public class AgentWorkerController {

    private final AgentWorkerService agentWorkerService;

    public AgentWorkerController(AgentWorkerService agentWorkerService) {
        this.agentWorkerService = agentWorkerService;
    }

    @PostMapping
    public ResponseEntity<AgentWorkerResponseDTO> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AgentWorkerCreateRequestDTO request) {
        return ResponseEntity.ok(agentWorkerService.create(user.getTenant(), request));
    }

    @GetMapping
    public ResponseEntity<List<AgentWorkerResponseDTO>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(agentWorkerService.listByTenant(user.getTenant()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentWorkerResponseDTO> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody AgentWorkerUpdateRequestDTO request) {
        return ResponseEntity.ok(agentWorkerService.update(id, user.getTenant(), request));
    }

    @PostMapping("/{id}/force-telemetry")
    public ResponseEntity<Void> forceTelemetry(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        agentWorkerService.forceTelemetry(id, user.getTenant());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/{id}/agent-bundle", produces = "application/zip")
    public ResponseEntity<byte[]> getAgentBundle(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "windows") String os) {
        
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        byte[] bundle = agentWorkerService.generateAgentBundle(id, user.getTenant(), os, baseUrl);
        
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"dba-agent-" + os + "-bundle.zip\"")
                .body(bundle);
    }
}
