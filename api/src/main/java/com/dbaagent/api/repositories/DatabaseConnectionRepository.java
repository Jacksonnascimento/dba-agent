package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseConnectionRepository extends JpaRepository<DatabaseConnection, Long> {
    List<DatabaseConnection> findByTenantAndIsActiveTrue(Tenant tenant);

    Optional<DatabaseConnection> findByIdAndTenant(Long id, Tenant tenant);
}
