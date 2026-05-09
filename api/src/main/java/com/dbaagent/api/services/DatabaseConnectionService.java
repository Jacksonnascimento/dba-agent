package com.dbaagent.api.services;

import com.dbaagent.api.dtos.DatabaseConnectionCreateRequestDTO;
import com.dbaagent.api.dtos.DatabaseConnectionResponseDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DatabaseConnectionService {

    private final DatabaseConnectionRepository repository;

    public DatabaseConnectionService(DatabaseConnectionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DatabaseConnectionResponseDTO create(Tenant tenant, DatabaseConnectionCreateRequestDTO request) {
        DatabaseConnection connection = new DatabaseConnection();
        connection.setTenant(tenant);
        connection.setName(request.getName().trim());
        connection.setDbEngine(request.getDbEngine().trim());
        connection.setConnectionUri(request.getConnectionUri().trim());
        connection.setActive(true);
        return map(repository.save(connection));
    }

    @Transactional(readOnly = true)
    public List<DatabaseConnectionResponseDTO> listActive(Tenant tenant) {
        return repository.findByTenantAndIsActiveTrue(tenant).stream().map(this::map).toList();
    }

    private DatabaseConnectionResponseDTO map(DatabaseConnection c) {
        return DatabaseConnectionResponseDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .dbEngine(c.getDbEngine())
                .active(c.getActive())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
