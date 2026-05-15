package com.dbaagent.api.services;

import com.dbaagent.api.dtos.AgentWorkerCreateRequestDTO;
import com.dbaagent.api.dtos.AgentWorkerResponseDTO;
import com.dbaagent.api.dtos.AgentWorkerUpdateRequestDTO;
import com.dbaagent.api.dtos.DatabaseConnectionResponseDTO;
import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.AgentWorkerRepository;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class AgentWorkerService {

    private final AgentWorkerRepository repository;
    private final DatabaseConnectionRepository dbRepository;
    private final OptimizationSuggestionRepository suggestionRepository;

    public AgentWorkerService(AgentWorkerRepository repository,
                              DatabaseConnectionRepository dbRepository,
                              OptimizationSuggestionRepository suggestionRepository) {
        this.repository = repository;
        this.dbRepository = dbRepository;
        this.suggestionRepository = suggestionRepository;
    }

    @Transactional(readOnly = true)
    public List<AgentWorkerResponseDTO> listByTenant(Tenant tenant) {
        return repository.findByTenant(tenant).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AgentWorkerResponseDTO create(Tenant tenant, AgentWorkerCreateRequestDTO request) {
        AgentWorker worker = new AgentWorker();
        worker.setTenant(tenant);
        worker.setName(request.getName());
        worker.setSnapshotIntervalMinutes(request.getSnapshotIntervalMinutes());

        if (request.getDatabaseConnectionIds() != null && !request.getDatabaseConnectionIds().isEmpty()) {
            Set<DatabaseConnection> dbs = new HashSet<>(dbRepository.findAllById(request.getDatabaseConnectionIds()));
            // Filtrar apenas os que são do mesmo tenant (segurança)
            dbs.removeIf(db -> !db.getTenant().getId().equals(tenant.getId()));
            worker.setDatabases(dbs);
        }

        return mapToDTO(repository.save(worker));
    }

    @Transactional
    public AgentWorkerResponseDTO update(Long id, Tenant tenant, AgentWorkerUpdateRequestDTO request) {
        AgentWorker worker = repository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente não encontrado"));

        worker.setName(request.getName());
        worker.setSnapshotIntervalMinutes(request.getSnapshotIntervalMinutes());

        if (request.getDatabaseConnectionIds() != null) {
            Set<DatabaseConnection> dbs = new HashSet<>(dbRepository.findAllById(request.getDatabaseConnectionIds()));
            dbs.removeIf(db -> !db.getTenant().getId().equals(tenant.getId()));
            worker.setDatabases(dbs);
        } else {
            worker.getDatabases().clear();
        }

        return mapToDTO(repository.save(worker));
    }

    @Transactional
    public void forceTelemetry(Long id, Tenant tenant) {
        AgentWorker worker = repository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente não encontrado"));

        for (DatabaseConnection db : worker.getDatabases()) {
            OptimizationSuggestion hiddenTask = new OptimizationSuggestion();
            hiddenTask.setTenant(tenant);
            hiddenTask.setDatabaseConnection(db);
            hiddenTask.setDatabaseName(db.getName());
            hiddenTask.setSchemaHash("FORCE_TELEMETRY");
            hiddenTask.setContextHash("FORCE_TELEMETRY_" + System.currentTimeMillis());
            hiddenTask.setStatus(SuggestionStatus.APPROVED);
            hiddenTask.setSuggestionText("Extração manual forçada pelo usuário.");
            hiddenTask.setDiagnosis("Comando de coleta manual (Force Telemetry).");
            hiddenTask.setUpScript("EXTRAÇÃO");
            hiddenTask.setDownScript("EXTRAÇÃO");
            
            suggestionRepository.save(hiddenTask);
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateAgentBundle(Long id, Tenant tenant, String os, String apiUrl) {
        AgentWorker worker = repository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agente não encontrado"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            // 1. Write the executable
            String binName = os.equalsIgnoreCase("windows") ? "agent-windows.exe" : "agent-linux";
            ClassPathResource resource = new ClassPathResource("agent-binaries/" + binName);
            if (resource.exists()) {
                ZipEntry binEntry = new ZipEntry(binName);
                zos.putNextEntry(binEntry);
                try (InputStream is = resource.getInputStream()) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            } else {
                ZipEntry errEntry = new ZipEntry("README_ERROR.txt");
                zos.putNextEntry(errEntry);
                zos.write("Binário do agente não encontrado no servidor.".getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // CORREÇÃO: Força o uso de HTTPS caso o ambiente não seja localhost
            if (apiUrl != null && !apiUrl.contains("localhost") && apiUrl.startsWith("http://")) {
                apiUrl = apiUrl.replaceFirst("http://", "https://");
            }

            // 2. Write the install and uninstall scripts with arguments injected!
            String baseApiUrl = apiUrl + "/api/v1";
            
            if (os.equalsIgnoreCase("windows")) {
                ZipEntry installEntry = new ZipEntry("install_service.bat");
                zos.putNextEntry(installEntry);
                String installContent = String.format("@echo off\ncd /d \"%%~dp0\"\necho Instalando o DBA Agent...\nagent-windows.exe -service install -api \"%s\" -token \"%s\"\nagent-windows.exe -service start\necho Servico instalado e rodando com sucesso!\npause", baseApiUrl, worker.getWorkerToken());
                zos.write(installContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                ZipEntry uninstallEntry = new ZipEntry("uninstall_service.bat");
                zos.putNextEntry(uninstallEntry);
                String uninstallContent = "@echo off\ncd /d \"%~dp0\"\necho Removendo o DBA Agent...\nagent-windows.exe -service stop\nagent-windows.exe -service uninstall\necho Servico removido com sucesso!\npause";
                zos.write(uninstallContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            } else {
                ZipEntry installEntry = new ZipEntry("install_service.sh");
                zos.putNextEntry(installEntry);
                String installContent = String.format("#!/bin/bash\nchmod +x agent-linux\n./agent-linux -service install -api \"%s\" -token \"%s\"\n./agent-linux -service start\necho 'Servico instalado e rodando!'\n", baseApiUrl, worker.getWorkerToken());
                zos.write(installContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                ZipEntry uninstallEntry = new ZipEntry("uninstall_service.sh");
                zos.putNextEntry(uninstallEntry);
                String uninstallContent = "#!/bin/bash\n./agent-linux -service stop\n./agent-linux -service uninstall\necho 'Servico removido!'\n";
                zos.write(uninstallContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar bundle: " + e.getMessage());
        }
    }

    private AgentWorkerResponseDTO mapToDTO(AgentWorker worker) {
        List<DatabaseConnectionResponseDTO> dbs = worker.getDatabases().stream().map(db -> 
            DatabaseConnectionResponseDTO.builder()
                .id(db.getId())
                .name(db.getName())
                .dbEngine(db.getDbEngine())
                .active(db.getActive())
                .createdAt(db.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return AgentWorkerResponseDTO.builder()
                .id(worker.getId())
                .name(worker.getName())
                .workerToken(worker.getWorkerToken())
                .snapshotIntervalMinutes(worker.getSnapshotIntervalMinutes())
                .databases(dbs)
                .createdAt(worker.getCreatedAt())
                .build();
    }
}