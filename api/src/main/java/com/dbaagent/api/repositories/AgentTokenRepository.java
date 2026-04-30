package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.AgentToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AgentTokenRepository extends JpaRepository<AgentToken, Long> {
    
    // JOIN FETCH para evitar o erro de LazyLoad quando o filtro validar o Agente
    @Query("SELECT a FROM AgentToken a JOIN FETCH a.tenant WHERE a.token = :token AND a.isActive = true")
    Optional<AgentToken> findByTokenWithTenant(String token);
}