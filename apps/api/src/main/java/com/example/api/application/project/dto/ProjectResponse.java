package com.example.api.application.project.dto;

import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.Project;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for project operations.
 */
public record ProjectResponse(
    String id,
    String organizationId,
    String name,
    String slug,
    String description,
    String status,
    LocalDateTime createdAt,
    List<EnvironmentDto> environments,
    List<ApiKeyDto> apiKeys
) {

    public record EnvironmentDto(
        String id,
        String environmentType,
        LocalDateTime createdAt
    ) {
        public static EnvironmentDto from(Environment environment) {
            return new EnvironmentDto(
                environment.getId().value(),
                environment.getEnvironmentType().name(),
                environment.getCreatedAt()
            );
        }
    }

    public record ApiKeyDto(
        String id,
        String environmentId,
        String environmentType,
        String name,
        String keyPrefix,
        String rawKey,
        LocalDateTime createdAt
    ) {
        public static ApiKeyDto from(ApiKey apiKey, EnvironmentType environmentType, String rawKey) {
            return new ApiKeyDto(
                apiKey.getId().value(),
                apiKey.getEnvironmentId().value(),
                environmentType.name(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                rawKey,
                apiKey.getCreatedAt()
            );
        }

        public static ApiKeyDto from(ApiKey apiKey, EnvironmentType environmentType) {
            return new ApiKeyDto(
                apiKey.getId().value(),
                apiKey.getEnvironmentId().value(),
                environmentType.name(),
                apiKey.getName(),
                apiKey.getKeyPrefix(),
                null,
                apiKey.getCreatedAt()
            );
        }
    }

    public static ProjectResponse from(Project project, List<Environment> environments, List<ApiKeyWithType> apiKeys) {
        return new ProjectResponse(
            project.getId().value(),
            project.getOrganizationId().value(),
            project.getName(),
            project.getSlug(),
            project.getDescription(),
            project.getStatus().name(),
            project.getCreatedAt(),
            environments.stream().map(EnvironmentDto::from).toList(),
            apiKeys.stream().map(ak -> ApiKeyDto.from(ak.apiKey(), ak.environmentType(), ak.rawKey())).toList()
        );
    }

    public static ProjectResponse from(Project project, List<Environment> environments) {
        return new ProjectResponse(
            project.getId().value(),
            project.getOrganizationId().value(),
            project.getName(),
            project.getSlug(),
            project.getDescription(),
            project.getStatus().name(),
            project.getCreatedAt(),
            environments.stream().map(EnvironmentDto::from).toList(),
            List.of()
        );
    }

    public record ApiKeyWithType(ApiKey apiKey, EnvironmentType environmentType, String rawKey) {}
}
