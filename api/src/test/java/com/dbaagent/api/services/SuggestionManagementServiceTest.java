package com.dbaagent.api.services;

import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.OptimizationSuggestion;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.enums.SuggestionStatus;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.repositories.OptimizationSuggestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SuggestionManagementServiceTest {

    @Mock
    private OptimizationSuggestionRepository repository;
    @Mock
    private DatabaseConnectionRepository databaseConnectionRepository;
    @Mock
    private SuggestionAuditLogService auditLogService;

    @InjectMocks
    private SuggestionManagementService service;

    @Test
    void approveShouldFailWhenSuggestionNotPending() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        User user = new User();
        user.setId(10L);
        user.setTenant(tenant);

        DatabaseConnection db = new DatabaseConnection();
        db.setId(100L);
        db.setTenant(tenant);
        db.setName("db");

        OptimizationSuggestion suggestion = OptimizationSuggestion.builder()
                .id(7L)
                .tenant(tenant)
                .databaseConnection(db)
                .status(SuggestionStatus.EXECUTED)
                .databaseName("db")
                .tableName("t")
                .suggestionText("x")
                .upScript("u")
                .downScript("d")
                .contextHash("c")
                .schemaHash("s")
                .build();

        when(repository.findById(7L)).thenReturn(Optional.of(suggestion));

        assertThrows(ResponseStatusException.class, () -> service.approve(7L, tenant, user));
        verify(repository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void approveShouldPersistWhenPending() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        User user = new User();
        user.setId(10L);
        user.setTenant(tenant);

        DatabaseConnection db = new DatabaseConnection();
        db.setId(100L);
        db.setTenant(tenant);
        db.setName("db");

        OptimizationSuggestion suggestion = OptimizationSuggestion.builder()
                .id(8L)
                .tenant(tenant)
                .databaseConnection(db)
                .status(SuggestionStatus.PENDING)
                .databaseName("db")
                .tableName("t")
                .suggestionText("x")
                .upScript("u")
                .downScript("d")
                .contextHash("c")
                .schemaHash("s")
                .build();

        when(repository.findById(8L)).thenReturn(Optional.of(suggestion));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.approve(8L, tenant, user);

        ArgumentCaptor<OptimizationSuggestion> captor = ArgumentCaptor.forClass(OptimizationSuggestion.class);
        verify(repository).save(captor.capture());
        assertEquals(SuggestionStatus.APPROVED, captor.getValue().getStatus());
        verify(auditLogService).log(any(), eq("APPROVED"), eq("USER"), eq("user:10"), any());
    }
}

