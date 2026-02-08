package com.example.api.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Configuration properties for frontend integration.
 */
@Component
@ConfigurationProperties(prefix = "app.frontend")
public class FrontendCorsProperties {

    private String allowedOrigins = "";

    public List<String> allowedOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = Objects.requireNonNullElse(allowedOrigins, "");
    }
}
