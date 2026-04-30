package com.dbaagent.api.security;

import com.dbaagent.api.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    // Chave secreta que deve ficar no seu application.properties (aqui está hardcoded para teste)
    // NUNCA vaze isso em produção!
    private static final String SECRET = "DbaAgentSuperSecretKeyWithAtLeast32CharactersForJWT!!!";
    private static final long EXPIRATION_TIME = 86400000; // 24 horas em milissegundos

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole())
                .claim("tenantId", user.getTenant().getId()) // Injetamos o ID da empresa no Token!
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractEmail(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}