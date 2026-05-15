package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.AgentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AgentTokenRepository extends JpaRepository<AgentToken, Long> {
    
    @Query("""
            SELECT a
            FROM AgentToken a
            JOIN FETCH a.tenant
            JOIN FETCH a.databaseConnection
            WHERE a.token = :token
              AND a.isActive = true
            """)
    Optional<AgentToken> findByTokenWithTenant(String token);

    @Query("""
            SELECT a
            FROM AgentToken a
            JOIN FETCH a.databaseConnection
            WHERE a.tenant = :tenant
            ORDER BY a.createdAt DESC
            """)
    java.util.List<AgentToken> findByTenantOrderByCreatedAtDesc(com.dbaagent.api.entities.Tenant tenant);

    @Query("""
            SELECT a
            FROM AgentToken a
            WHERE a.tenant = :tenant
              AND a.databaseConnection = :databaseConnection
              AND a.isActive = true
            """)
    java.util.List<AgentToken> findByTenantAndDatabaseConnection(com.dbaagent.api.entities.Tenant tenant, com.dbaagent.api.entities.DatabaseConnection databaseConnection);
}