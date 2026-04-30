package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentTokenRequestDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.services.AgentTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent-tokens")
public class AgentTokenController {

    private final AgentTokenService agentTokenService;

    public AgentTokenController(AgentTokenService agentTokenService) {
        this.agentTokenService = agentTokenService;
    }

    @PostMapping
    public ResponseEntity<?> createAgentToken(
            @Valid @RequestBody AgentTokenRequestDTO request,
            @AuthenticationPrincipal User loggedUser) {

        // O usuário logado já tem o Tenant dele "pendurado" na memória (graças ao JOIN FETCH)
        AgentToken newToken = agentTokenService.generateTokenForTenant(
                request.getDescription(), 
                loggedUser.getTenant()
        );

        // Devolvemos o token limpo para o usuário poder copiar e colar no Agente
        return ResponseEntity.ok(Map.of(
                "id", newToken.getId(),
                "token", newToken.getToken(),
                "description", newToken.getDescription(),
                "createdAt", newToken.getCreatedAt()
        ));
    }
}