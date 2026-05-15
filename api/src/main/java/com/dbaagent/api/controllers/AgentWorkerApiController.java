package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AgentConfigResponseDTO;
import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.repositories.AgentTokenRepository;
import com.dbaagent.api.repositories.AgentWorkerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/agent-workers/me")
public class AgentWorkerApiController {

    private final AgentWorkerRepository workerRepository;
    private final AgentTokenRepository tokenRepository;

    public AgentWorkerApiController(AgentWorkerRepository workerRepository, AgentTokenRepository tokenRepository) {
        this.workerRepository = workerRepository;
        this.tokenRepository = tokenRepository;
    }

    @GetMapping("/config")
    @Transactional
    public ResponseEntity<List<AgentConfigResponseDTO>> getWorkerConfig(@RequestHeader("X-Worker-Token") String workerToken) {
        
        AgentWorker worker = workerRepository.findByWorkerToken(workerToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Worker Token inválido"));

        List<AgentConfigResponseDTO> configs = new ArrayList<>();

        for (DatabaseConnection db : worker.getDatabases()) {
            if (!db.getActive()) continue;

            Optional<AgentToken> tokenOpt = tokenRepository.findByTenantAndDatabaseConnection(worker.getTenant(), db).stream().findFirst();
            AgentToken dbToken;
            
            if (tokenOpt.isEmpty()) {
                dbToken = new AgentToken();
                dbToken.setTenant(worker.getTenant());
                dbToken.setDatabaseConnection(db);
                dbToken.setToken(java.util.UUID.randomUUID().toString());
                dbToken.setDescription("Auto-gerado para " + db.getName());
                dbToken.setActive(true); // CORRIGIDO: Lombok gera setActive() para boolean isActive/active
                tokenRepository.save(dbToken);
            } else {
                dbToken = tokenOpt.get();
            }

            AgentConfigResponseDTO dto = new AgentConfigResponseDTO();
            dto.setName(db.getName());
            dto.setDbEngine(db.getDbEngine());
            dto.setConnectionUri(db.getConnectionUri());
            dto.setSnapshotIntervalMinutes(worker.getSnapshotIntervalMinutes());
            dto.setAgentToken(dbToken.getToken());

            configs.add(dto);
        }

        return ResponseEntity.ok(configs);
    }
}