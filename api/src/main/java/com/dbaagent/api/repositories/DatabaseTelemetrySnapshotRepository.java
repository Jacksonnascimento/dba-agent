package com.dbaagent.api.repositories;

import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.DatabaseTelemetrySnapshot;
import com.dbaagent.api.entities.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DatabaseTelemetrySnapshotRepository extends JpaRepository<DatabaseTelemetrySnapshot, Long> {
    Optional<DatabaseTelemetrySnapshot> findTop1ByTenantAndDatabaseConnectionOrderByCollectedAtDesc(
            Tenant tenant,
            DatabaseConnection databaseConnection);

    Page<DatabaseTelemetrySnapshot> findByTenantAndDatabaseConnectionOrderByCollectedAtDesc(
            Tenant tenant,
            DatabaseConnection databaseConnection,
            Pageable pageable);
}
