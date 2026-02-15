package com.example.api.infrastructure.security;

import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * API key authentication filter for SDK endpoints.
 * Validates X-API-Key header and resolves project/environment context.
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String PROJECT_CONTEXT_ATTR = "projectContext";

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            final var apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey != null && !apiKey.isBlank()) {
                final var keyHash = hashApiKey(apiKey);
                final var apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

                if (apiKeyOpt.isPresent()) {
                    final var key = apiKeyOpt.get();
                    if (!key.isRevoked()) {
                        final var context = new ProjectContext(key.getProjectId(), key.getEnvironmentId());
                        request.setAttribute(PROJECT_CONTEXT_ATTR, context);
                    }
                }
            }
        } finally {
            filterChain.doFilter(request, response);
            // Clear context after request processing
            request.removeAttribute(PROJECT_CONTEXT_ATTR);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final var path = request.getRequestURI();
        // Skip org endpoints and non-SDK endpoints
        if (path.startsWith("/api/v1/org/")) {
            return true;
        }
        if (path.startsWith("/api/v1/customers/")) {
            return true;
        }
        // Filter SDK auth endpoints and end-user endpoints
        return !(path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/end-users/"));
    }

    private String hashApiKey(String apiKey) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final var hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Retrieves the project context from the request if available.
     */
    public static ProjectContext getContext(HttpServletRequest request) {
        return (ProjectContext) request.getAttribute(PROJECT_CONTEXT_ATTR);
    }
}
