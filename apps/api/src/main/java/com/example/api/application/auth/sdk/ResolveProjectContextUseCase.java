package com.example.api.application.auth.sdk;

import com.example.api.application.auth.sdk.dto.ProjectContextDto;
import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

/**
 * Use case for resolving project context from an API key.
 */
@Component
public class ResolveProjectContextUseCase {

    private final ApiKeyRepository apiKeyRepository;

    public ResolveProjectContextUseCase(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Resolves project and environment context from a raw API key.
     *
     * @param rawApiKey the raw API key from request header
     * @return project context if valid key found, empty otherwise
     */
    public Optional<ProjectContextDto> execute(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            return Optional.empty();
        }

        final var keyHash = hashApiKey(rawApiKey);
        return apiKeyRepository.findByKeyHash(keyHash)
                .filter(key -> !key.isRevoked())
                .map(key -> new ProjectContextDto(key.getProjectId(), key.getEnvironmentId()));
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
}
