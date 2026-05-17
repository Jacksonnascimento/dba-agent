package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.AiPromptPreviewResponseDTO;
import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.AgentWorkerRepository;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.services.AiPromptService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai-prompt")
public class AiPromptController {

    private final AiPromptService aiPromptService;
    private final AgentWorkerRepository agentWorkerRepository;
    private final DatabaseConnectionRepository databaseConnectionRepository;

    public AiPromptController(
            AiPromptService aiPromptService,
            AgentWorkerRepository agentWorkerRepository,
            DatabaseConnectionRepository databaseConnectionRepository) {
        this.aiPromptService = aiPromptService;
        this.agentWorkerRepository = agentWorkerRepository;
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    @GetMapping("/contract")
    public ResponseEntity<Map<String, String>> contract(@RequestParam(defaultValue = "SQL Server") String dbEngine) {
        return ResponseEntity.ok(Map.of(
                "dbEngine", dbEngine,
                "contract", aiPromptService.buildContractOnly(dbEngine)));
    }

    @GetMapping("/preview")
    public ResponseEntity<AiPromptPreviewResponseDTO> preview(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long agentWorkerId,
            @RequestParam(required = false) Long databaseConnectionId) {

        AgentWorker agentWorker = null;
        DatabaseConnection databaseConnection = null;
        String dbEngine = "SQL Server";

        if (agentWorkerId != null) {
            agentWorker = agentWorkerRepository.findByIdAndTenantWithDatabases(agentWorkerId, user.getTenant())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente não encontrado."));
        }

        if (databaseConnectionId != null) {
            databaseConnection = databaseConnectionRepository.findByIdAndTenant(databaseConnectionId, user.getTenant())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Banco não encontrado."));
            dbEngine = databaseConnection.getDbEngine();
        } else if (agentWorker != null && !agentWorker.getDatabases().isEmpty()) {
            dbEngine = agentWorker.getDatabases().iterator().next().getDbEngine();
        }

        if (databaseConnection == null) {
            databaseConnection = new DatabaseConnection();
            databaseConnection.setName("(banco não selecionado)");
            databaseConnection.setDbEngine(dbEngine);
        }

        String preview = aiPromptService.buildPreview(dbEngine, agentWorker, databaseConnection);

        return ResponseEntity.ok(AiPromptPreviewResponseDTO.builder()
                .dbEngine(dbEngine)
                .preview(preview)
                .build());
    }
}
