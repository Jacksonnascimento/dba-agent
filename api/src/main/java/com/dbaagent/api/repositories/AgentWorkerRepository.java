package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.AgentWorker;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentWorkerRepository extends JpaRepository<AgentWorker, Long> {
    List<AgentWorker> findByTenant(Tenant tenant);
    Optional<AgentWorker> findByIdAndTenant(Long id, Tenant tenant);
    Optional<AgentWorker> findByWorkerToken(String workerToken);
}
