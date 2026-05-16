package com.dbaagent.api.controllers;

import com.dbaagent.api.entities.User;
import com.dbaagent.api.repositories.UserRepository;
import com.dbaagent.api.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> listUsers(Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado");
        }
        List<User> users = userService.findAll();
        List<Map<String, Object>> response = users.stream().map(this::mapToDto).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> body, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado");
        }
        try {
            Long tenantId = body.get("tenantId") != null
                    ? Long.valueOf(body.get("tenantId").toString())
                    : null;
            User user = userService.createUser(
                    (String) body.get("name"),
                    (String) body.get("email"),
                    (String) body.get("password"),
                    body.getOrDefault("role", "ROLE_CLIENT").toString(),
                    tenantId
            );
            return ResponseEntity.ok(mapToDto(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication authentication) {
        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acesso negado");
        }
        try {
            userService.updateUser(
                    id,
                    (String) body.get("name"),
                    (String) body.get("email"),
                    (String) body.get("role"),
                    body.containsKey("active") ? (Boolean) body.get("active") : null
            );
            User updatedUser = userRepository.findByIdWithTenant(id)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
            return ResponseEntity.ok(mapToDto(updatedUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication authentication) {
        // Pega o usuário logado
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário logado não encontrado"));

        // Se for alterar a própria senha ou se for ADMIN
        if (!currentUser.getId().equals(id) && !isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Você não tem permissão para alterar a senha deste usuário.");
        }

        try {
            userService.changePassword(id, body.get("password"));
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changeOwnPassword(@RequestBody Map<String, String> body, Authentication authentication) {
        try {
            userService.changeOwnPassword(authentication.getName(), body.get("password"));
            return ResponseEntity.ok(Map.of("message", "Senha alterada com sucesso"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Map<String, Object> mapToDto(User user) {
        Map<String, Object> dto = new java.util.HashMap<>();
        dto.put("id", user.getId());
        dto.put("name", user.getName());
        dto.put("email", user.getEmail());
        dto.put("role", user.getRole());
        dto.put("active", user.getActive());
        dto.put("createdAt", user.getCreatedAt());
        if (user.getTenant() != null) {
            dto.put("tenantId", user.getTenant().getId());
            dto.put("tenantName", user.getTenant().getName());
        }
        return dto;
    }
}
