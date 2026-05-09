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
}