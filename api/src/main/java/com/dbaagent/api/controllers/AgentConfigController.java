package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentConfigResponseDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.DatabaseConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/agent/config")
public class AgentConfigController {

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<AgentConfigResponseDTO> getConfig() {
        AgentToken agentToken = requireAgentToken();
        DatabaseConnection dbConnection = agentToken.getDatabaseConnection();

        if (!dbConnection.getActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Conexão de banco inativa.");
        }

        AgentConfigResponseDTO dto = new AgentConfigResponseDTO();
        dto.setName(dbConnection.getName());
        dto.setDbEngine(dbConnection.getDbEngine());
        dto.setConnectionUri(dbConnection.getConnectionUri()); // Seu CryptoConverter já cuida da decifragem aqui
        dto.setSnapshotIntervalMinutes(dbConnection.getSnapshotIntervalMinutes());

        return ResponseEntity.ok(dto);
    }

    private static AgentToken requireAgentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AgentToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Agente não autenticado.");
        }
        return (AgentToken) auth.getPrincipal();
    }
}