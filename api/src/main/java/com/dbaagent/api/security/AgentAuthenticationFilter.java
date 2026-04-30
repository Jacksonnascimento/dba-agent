package com.dbaagent.api.security;

import com.dbaagent.api.entities.AgentToken;
import com.dbaagent.api.repositories.AgentTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

@Component
public class AgentAuthenticationFilter extends OncePerRequestFilter {

    private final AgentTokenRepository agentTokenRepository;

    public AgentAuthenticationFilter(AgentTokenRepository agentTokenRepository) {
        this.agentTokenRepository = agentTokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Só executa esse filtro se a rota for exclusiva de agentes
        if (!request.getRequestURI().startsWith("/api/v1/agent/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String tokenHeader = request.getHeader("X-Agent-Token");

        if (tokenHeader != null && !tokenHeader.isEmpty()) {
            Optional<AgentToken> agentOpt = agentTokenRepository.findByTokenWithTenant(tokenHeader);

            if (agentOpt.isPresent()) {
                AgentToken agentToken = agentOpt.get();
                // Dá o crachá oficial de "Robô" para essa requisição
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_AGENT");
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        agentToken, null, Collections.singletonList(authority)
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}