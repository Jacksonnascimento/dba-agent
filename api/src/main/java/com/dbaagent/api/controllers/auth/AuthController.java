package com.dbaagent.api.controllers.auth;

import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.UserRepository;
import com.dbaagent.api.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        User user = userRepository.findByEmail(email).orElse(null);

        // Verifica se o usuário existe e se a senha digitada bate com o hash salvo no banco
        if (user != null && passwordEncoder.matches(password, user.getPasswordHash())) {
            // Sucesso! Gera o crachá e entrega pro usuário
            String token = jwtService.generateToken(user);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "role", user.getRole(),
                    "tenantId", user.getTenant().getId()
            ));
        }

        // Se errou a senha ou email, manda dar meia volta
        return ResponseEntity.status(401).body(Map.of("erro", "Credenciais inválidas"));
    }
}