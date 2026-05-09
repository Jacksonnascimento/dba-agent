package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.DatabaseConnectionCreateRequestDTO;
import com.dbaagent.api.dtos.DatabaseConnectionResponseDTO;
import com.dbaagent.api.dtos.DatabaseTelemetrySnapshotResponseDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.services.DatabaseTelemetrySnapshotService;
import com.dbaagent.api.services.DatabaseConnectionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database-connections")
public class DatabaseConnectionController {

    private final DatabaseConnectionService databaseConnectionService;
    private final DatabaseTelemetrySnapshotService snapshotService;
    private final DatabaseConnectionRepository databaseConnectionRepository;

    public DatabaseConnectionController(
            DatabaseConnectionService databaseConnectionService,
            DatabaseTelemetrySnapshotService snapshotService,
            DatabaseConnectionRepository databaseConnectionRepository) {
        this.databaseConnectionService = databaseConnectionService;
        this.snapshotService = snapshotService;
        this.databaseConnectionRepository = databaseConnectionRepository;
    }

    @PostMapping
    public ResponseEntity<DatabaseConnectionResponseDTO> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody DatabaseConnectionCreateRequestDTO request) {

        return ResponseEntity.ok(databaseConnectionService.create(user.getTenant(), request));
    }

    @GetMapping
    public ResponseEntity<List<DatabaseConnectionResponseDTO>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(databaseConnectionService.listActive(user.getTenant()));
    }

    @GetMapping("/{id}/snapshots")
    public ResponseEntity<Page<DatabaseTelemetrySnapshotResponseDTO>> listSnapshots(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DatabaseConnection db = databaseConnectionRepository
                .findByIdAndTenant(id, user.getTenant())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Database connection não encontrada."));

        Page<DatabaseTelemetrySnapshotResponseDTO> response =
                snapshotService.listSnapshots(user.getTenant(), db, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(response);
    }
}
