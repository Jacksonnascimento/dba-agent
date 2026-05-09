package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.SuggestionAuditLog;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionAuditLogRepository extends JpaRepository<SuggestionAuditLog, Long> {
    Page<SuggestionAuditLog> findByTenantOrderByCreatedAtDesc(Tenant tenant, Pageable pageable);

    Page<SuggestionAuditLog> findByTenantAndDatabaseConnectionOrderByCreatedAtDesc(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            Pageable pageable);
}

