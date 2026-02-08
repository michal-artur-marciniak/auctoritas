package com.example.api.application.apikey.dto;

import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.environment.EnvironmentType;

import java.time.LocalDateTime;

/**
 * Response DTO for rotated API key (includes raw key - shown only once).
 */
public record RotatedApiKeyResponse(
    String id,
    String environmentId,
    String environmentType,
    String name,
    String keyPrefix,
    String rawKey,
    LocalDateTime createdAt
) {
    public static RotatedApiKeyResponse from(ApiKey apiKey, EnvironmentType environmentType, String rawKey) {
        return new RotatedApiKeyResponse(
            apiKey.getId().value(),
            apiKey.getEnvironmentId().value(),
            environmentType.name(),
            apiKey.getName(),
            apiKey.getKeyPrefix(),
            rawKey,
            apiKey.getCreatedAt()
        );
    }
}
