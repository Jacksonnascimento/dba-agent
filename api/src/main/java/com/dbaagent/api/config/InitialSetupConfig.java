package com.dbaagent.api.config;

import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.TenantRepository;
import com.dbaagent.api.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitialSetupConfig implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public InitialSetupConfig(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            Tenant tenant;
            if (tenantRepository.count() == 0) {
                tenant = new Tenant();
                tenant.setName("Tenant Principal");
                tenant = tenantRepository.save(tenant);
            } else {
                tenant = tenantRepository.findAll().get(0);
            }

            User master = new User();
            master.setName("Admin Master");
            master.setEmail("master");
            master.setPasswordHash(passwordEncoder.encode("1"));
            master.setRole("ROLE_ADMIN");
            master.setTenant(tenant);
            master.setActive(true);

            userRepository.save(master);
            System.out.println(">>> Usuário master criado com sucesso (email: master, senha: 1) <<<");
        }
    }
}
