package com.dbaagent.api.services;

import com.dbaagent.api.entities.Tenant;
import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.TenantRepository;
import com.dbaagent.api.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> findAll() {
        return userRepository.findAllWithTenant();
    }

    public User createUser(String name, String email, String password, String role, Long tenantId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("E-mail já está em uso.");
        }

        Tenant tenant;
        if (tenantId != null) {
            tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não encontrado."));
        } else {
            // fallback para garantir compatibilidade com DataLoader/InitialSetupConfig
            List<Tenant> tenants = tenantRepository.findAll();
            if (tenants.isEmpty()) {
                tenant = new Tenant();
                tenant.setName("Default Tenant");
                tenant = tenantRepository.save(tenant);
            } else {
                tenant = tenants.get(0);
            }
        }

        User user = new User();
        user.setTenant(tenant);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setActive(true);

        return userRepository.save(user);
    }

    public User changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public User changeOwnPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        return userRepository.save(user);
    }

    public User updateUser(Long userId, String name, String email, String role, Boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new IllegalArgumentException("E-mail já está em uso.");
        }

        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        if (active != null) {
            user.setActive(active);
        }
        return userRepository.save(user);
    }
}
