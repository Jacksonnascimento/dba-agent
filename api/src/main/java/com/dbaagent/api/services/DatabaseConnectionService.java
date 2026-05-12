package com.dbaagent.api.services;

import com.dbaagent.api.dtos.DatabaseConnectionCreateRequestDTO;
import com.dbaagent.api.dtos.DatabaseConnectionResponseDTO;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        // 1. Faz o Encode seguro do Usuário e da Senha
        String safeUser = URLEncoder.encode(request.getUsername().trim(), StandardCharsets.UTF_8);
        String safePassword = URLEncoder.encode(request.getPassword(), StandardCharsets.UTF_8);

        // 2. Define o protocolo baseado na Engine
        String protocol = request.getDbEngine().equalsIgnoreCase("PostgreSQL") ? "postgres" : "sqlserver";

        // 3. Constrói a URI completa com os parâmetros fixos de segurança (encrypt=disable)
        String builtUri = String.format("%s://%s:%s@%s:%d?database=%s&encrypt=disable",
                protocol,
                safeUser,
                safePassword,
                request.getHost().trim(),
                request.getPort(),
                request.getDatabase().trim()
        );

        // 4. Salva a URI formatada (O @Convert na Entidade cuidará da criptografia)
        connection.setConnectionUri(builtUri);
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