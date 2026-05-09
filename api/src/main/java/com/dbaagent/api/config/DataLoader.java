package com.dbaagent.api.config;

import com.dbaagent.api.entities.DatabaseConnection;
import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.DatabaseConnectionRepository;
import com.dbaagent.api.repositories.TenantRepository;
import com.dbaagent.api.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final DatabaseConnectionRepository databaseConnectionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            DatabaseConnectionRepository databaseConnectionRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.databaseConnectionRepository = databaseConnectionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            Tenant tenant = new Tenant();
            tenant.setName("Horizon AJ");
            tenant = tenantRepository.save(tenant);

            User admin = new User();
            admin.setName("Admin DBA Agent");
            admin.setEmail("admin@horizonaj.com");
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole("ROLE_ADMIN");
            admin.setActive(true);
            admin.setTenant(tenant);

            userRepository.save(admin);

            DatabaseConnection databaseConnection = new DatabaseConnection();
            databaseConnection.setTenant(tenant);
            databaseConnection.setName("sqlserver-default");
            databaseConnection.setDbEngine("SQL Server");
            databaseConnection.setConnectionUri("jdbc:sqlserver://localhost:1433;databaseName=master");
            databaseConnection.setActive(true);
            databaseConnectionRepository.save(databaseConnection);
        }
    }
}