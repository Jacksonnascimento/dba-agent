package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.SuggestionAuditLog;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SuggestionAuditLogRepository extends JpaRepository<SuggestionAuditLog, Long> {

    @Query("SELECT l FROM SuggestionAuditLog l " +
           "JOIN FETCH l.databaseConnection " +
           "JOIN FETCH l.suggestion " +
           "WHERE l.tenant = :tenant " +
           "ORDER BY l.createdAt DESC")
    Page<SuggestionAuditLog> findByTenantOrderByCreatedAtDesc(Tenant tenant, Pageable pageable);

    @Query("SELECT l FROM SuggestionAuditLog l " +
           "JOIN FETCH l.databaseConnection " +
           "JOIN FETCH l.suggestion " +
           "WHERE l.tenant = :tenant AND l.databaseConnection = :db " +
           "ORDER BY l.createdAt DESC")
    Page<SuggestionAuditLog> findByTenantAndDatabaseConnectionOrderByCreatedAtDesc(
            Tenant tenant,
            DatabaseConnection db,
            Pageable pageable);
}

