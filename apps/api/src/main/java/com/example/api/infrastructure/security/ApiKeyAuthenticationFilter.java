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
 * 
 * <p>This filter enforces that SDK routes (both auth and end-user endpoints)
 * require a valid, non-revoked API key. Revoked keys are rejected with 401.
 * Context is automatically cleared after each request.</p>
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
            
            // SDK routes require API key
            if (isSdkRoute(request.getRequestURI())) {
                if (apiKey == null || apiKey.isBlank()) {
                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "X-API-Key header is required");
                    return;
                }
                
                final var keyHash = hashApiKey(apiKey);
                final var apiKeyOpt = apiKeyRepository.findByKeyHash(keyHash);

                if (apiKeyOpt.isEmpty()) {
                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid API key");
                    return;
                }
                
                final var key = apiKeyOpt.get();
                if (key.isRevoked()) {
                    sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "API key has been revoked");
                    return;
                }
                
                // Valid key - set project context
                final var context = new ProjectContext(key.getProjectId(), key.getEnvironmentId());
                request.setAttribute(PROJECT_CONTEXT_ATTR, context);
            }
        } catch (Exception e) {
            logger.error("Error processing API key", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error processing API key");
            return;
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear context after request processing
            request.removeAttribute(PROJECT_CONTEXT_ATTR);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        final var path = request.getRequestURI();
        // Only filter SDK routes that require API key context
        // SDK auth endpoints moved to /api/v1/end-users/auth/**
        // SDK user endpoints at /api/v1/end-users/**
        return !(path.startsWith("/api/v1/end-users/"));
    }
    
    /**
     * Checks if the request path is an SDK route requiring API key validation.
     */
    private boolean isSdkRoute(String path) {
        return path.startsWith("/api/v1/end-users/");
    }
    
    /**
     * Sends an error response and logs the rejection.
     */
    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("API Key validation failed: " + message);
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
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
