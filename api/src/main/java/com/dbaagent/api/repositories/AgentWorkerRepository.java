package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentWorkerRepository extends JpaRepository<AgentWorker, Long> {
    List<AgentWorker> findByTenant(Tenant tenant);

    @Query("SELECT w FROM AgentWorker w LEFT JOIN FETCH w.databases WHERE w.id = :id AND w.tenant = :tenant")
    Optional<AgentWorker> findByIdAndTenantWithDatabases(@Param("id") Long id, @Param("tenant") Tenant tenant);

    Optional<AgentWorker> findByIdAndTenant(Long id, Tenant tenant);

    Optional<AgentWorker> findByWorkerToken(String workerToken);

    @Query("SELECT w FROM AgentWorker w LEFT JOIN FETCH w.databases WHERE w.workerToken = :workerToken")
    Optional<AgentWorker> findByWorkerTokenWithDatabases(@Param("workerToken") String workerToken);
}
