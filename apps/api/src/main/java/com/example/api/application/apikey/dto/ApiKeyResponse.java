package com.example.api.application.apikey.dto;

import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.environment.EnvironmentType;

import java.time.LocalDateTime;

/**
 * Response DTO for API key metadata (redacted - no raw key).
 */
public record ApiKeyResponse(
    String id,
    String environmentId,
    String environmentType,
    String name,
    String keyPrefix,
    LocalDateTime lastUsedAt,
    LocalDateTime revokedAt,
    LocalDateTime createdAt
) {
    public static ApiKeyResponse from(ApiKey apiKey, EnvironmentType environmentType) {
        return new ApiKeyResponse(
            apiKey.getId().value(),
            apiKey.getEnvironmentId().value(),
            environmentType.name(),
            apiKey.getName(),
            apiKey.getKeyPrefix(),
            apiKey.getLastUsedAt(),
            apiKey.getRevokedAt(),
            apiKey.getCreatedAt()
        );
    }
}
