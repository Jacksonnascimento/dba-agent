package com.dbaagent.api.config;

import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.TenantRepository;
import com.dbaagent.api.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
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
        }
    }
}