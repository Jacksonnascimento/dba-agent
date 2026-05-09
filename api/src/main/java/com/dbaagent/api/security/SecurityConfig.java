package com.dbaagent.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AgentAuthenticationFilter agentAuthFilter;
    private final SimpleRateLimitFilter rateLimitFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthFilter,
            AgentAuthenticationFilter agentAuthFilter,
            SimpleRateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.agentAuthFilter = agentAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Rotas Públicas
                .requestMatchers("/api/v1/auth/**").permitAll()
                
                // Rotas do Agente (a validação é feita internamente pelo AgentAuthenticationFilter via Header)
                .requestMatchers("/api/v1/agent/**").permitAll()
                
                // Rotas exclusivas de Administração (Gestão de Tenants, Usuários, etc)
                .requestMatchers("/api/v1/admin/**").hasAuthority("ROLE_ADMIN")
                
                // Qualquer outra rota exige estar autenticado (ROLE_ADMIN ou ROLE_CLIENT)
                .anyRequest().authenticated()
            )
            // Ordem dos filtros: rate limit -> agent auth -> jwt auth
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(agentAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(jwtAuthFilter, AgentAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}