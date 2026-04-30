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

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(TenantRepository tenantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Só cria se o banco estiver vazio
        if (tenantRepository.count() == 0) {
            Tenant tenant = new Tenant();
            tenant.setName("DBA Agent Matrix");
            tenant.setActive(true);
            tenant = tenantRepository.save(tenant);

            User user = new User();
            user.setTenant(tenant);
            user.setName("Neo DBA");
            user.setEmail("neo@dbaagent.com");
            // Aqui o próprio Spring vai gerar o Hash da maneira correta!
            user.setPasswordHash(passwordEncoder.encode("123456")); 
            user.setRole("ROLE_ADMIN");
            user.setActive(true);
            userRepository.save(user);

            System.out.println("✅ Usuário e Tenant de teste criados com sucesso pelo Spring!");
        }
    }
}