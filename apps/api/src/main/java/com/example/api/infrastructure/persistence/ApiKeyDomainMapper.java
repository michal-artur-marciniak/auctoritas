package com.example.api.infrastructure.persistence;

import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyId;
import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;

/**
 * Maps between domain {@link ApiKey} and JPA {@link ApiKeyJpaEntity}.
 */
final class ApiKeyDomainMapper {

    private ApiKeyDomainMapper() {
        // Utility class
    }

    static ApiKey toDomain(ApiKeyJpaEntity entity) {
        return new ApiKey(
                ApiKeyId.of(entity.getId()),
                ProjectId.of(entity.getProjectId()),
                EnvironmentId.of(entity.getEnvironmentId()),
                entity.getName(),
                entity.getKeyHash(),
                entity.getKeyPrefix(),
                entity.getLastUsedAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }

    static ApiKeyJpaEntity toEntity(ApiKey apiKey) {
        return new ApiKeyJpaEntity(
                apiKey.getId().value(),
                apiKey.getProjectId().value(),
                apiKey.getEnvironmentId().value(),
                apiKey.getName(),
                apiKey.getKeyHash(),
                apiKey.getKeyPrefix(),
                apiKey.getLastUsedAt(),
                apiKey.getRevokedAt(),
                apiKey.getCreatedAt()
        );
    }
}
