package com.dbaagent.api.controllers;

import com.dbaagent.api.dtos.TenantCreateRequestDTO;
import com.dbaagent.api.dtos.TenantResponseDTO;
import com.dbaagent.api.dtos.TenantUpdateRequestDTO;
import com.dbaagent.api.services.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<TenantResponseDTO>> listAll() {
        return ResponseEntity.ok(tenantService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TenantResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(tenantService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TenantResponseDTO> create(@Valid @RequestBody TenantCreateRequestDTO request) {
        return ResponseEntity.ok(tenantService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<TenantResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody TenantUpdateRequestDTO request) {
        return ResponseEntity.ok(tenantService.update(id, request));
    }
}
