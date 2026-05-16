package com.dbaagent.api.services;

import com.dbaagent.api.dtos.TenantCreateRequestDTO;
import com.dbaagent.api.dtos.TenantResponseDTO;
import com.dbaagent.api.dtos.TenantUpdateRequestDTO;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public List<TenantResponseDTO> listAll() {
        return tenantRepository.findAll().stream()
                .map(this::map)
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantResponseDTO findById(Long id) {
        return tenantRepository.findById(id)
                .map(this::map)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado."));
    }

    @Transactional
    public TenantResponseDTO create(TenantCreateRequestDTO request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.getName().trim());
        tenant.setActive(true);
        return map(tenantRepository.save(tenant));
    }

    @Transactional
    public TenantResponseDTO update(Long id, TenantUpdateRequestDTO request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant não encontrado."));

        tenant.setName(request.getName().trim());
        if (request.getActive() != null) {
            tenant.setActive(request.getActive());
        }
        return map(tenantRepository.save(tenant));
    }

    private TenantResponseDTO map(Tenant t) {
        return new TenantResponseDTO(t.getId(), t.getName(), t.getActive(), t.getCreatedAt());
    }
}
