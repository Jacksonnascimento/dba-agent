package com.dbaagent.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SimpleRateLimitFilter extends OncePerRequestFilter {

    private static final int LIMIT_PER_MINUTE = 120;
    private static final long WINDOW_SECONDS = 60L;

    private static final class CounterWindow {
        volatile long windowStartEpochSec;
        final AtomicInteger count = new AtomicInteger(0);
    }

    private final Map<String, CounterWindow> counters = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        if (!(uri.startsWith("/api/v1/agent/telemetry") || uri.startsWith("/api/v1/agent/tasks"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Agent-Token");
        String key = (token == null || token.isBlank()) ? "anon:" + request.getRemoteAddr() : "tok:" + token;
        long now = Instant.now().getEpochSecond();

        CounterWindow cw = counters.computeIfAbsent(key, k -> {
            CounterWindow n = new CounterWindow();
            n.windowStartEpochSec = now;
            return n;
        });

        synchronized (cw) {
            if ((now - cw.windowStartEpochSec) >= WINDOW_SECONDS) {
                cw.windowStartEpochSec = now;
                cw.count.set(0);
            }
            int current = cw.count.incrementAndGet();
            if (current > LIMIT_PER_MINUTE) {
                response.setStatus(429);
                response.getWriter().write("Rate limit exceeded for agent endpoint.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

