package com.dbaagent.api.security; // ou seu pacote de config

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Cors cors = new Cors();

    public Cors getCors() { return cors; }

    public static class Cors {
        private java.util.List<String> allowedOrigins;
        public java.util.List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(java.util.List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }
}