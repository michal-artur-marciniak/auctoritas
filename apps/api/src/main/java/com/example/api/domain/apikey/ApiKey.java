package com.example.api.domain.apikey;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.ProjectId;

import java.time.LocalDateTime;
import java.util.Objects;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Domain entity representing an API key for SDK authentication.
 */
public class ApiKey {

    private final ApiKeyId id;
    private final ProjectId projectId;
    private final EnvironmentId environmentId;
    private String name;
    private final String keyHash;
    private final String keyPrefix;
    private LocalDateTime lastUsedAt;
    private LocalDateTime revokedAt;
    private final LocalDateTime createdAt;

    public ApiKey(ApiKeyId id,
                  ProjectId projectId,
                  EnvironmentId environmentId,
                  String name,
                  String keyHash,
                  String keyPrefix,
                  LocalDateTime lastUsedAt,
                  LocalDateTime revokedAt,
                  LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "API Key ID required");
        this.projectId = Objects.requireNonNull(projectId, "Project ID required");
        this.environmentId = Objects.requireNonNull(environmentId, "Environment ID required");
        this.name = name;
        this.keyHash = Objects.requireNonNull(keyHash, "Key hash required");
        this.keyPrefix = Objects.requireNonNull(keyPrefix, "Key prefix required");
        this.lastUsedAt = lastUsedAt;
        this.revokedAt = revokedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
    }

    public static ApiKey create(ProjectId projectId, EnvironmentId environmentId, EnvironmentType environmentType, String name, String keyHash) {
        String prefix = environmentType == EnvironmentType.PROD ? "pk_prod_" : "pk_dev_";
        return new ApiKey(
                ApiKeyId.generate(),
                projectId,
                environmentId,
                name,
                keyHash,
                prefix,
                null,
                null,
                LocalDateTime.now()
        );
    }

    public static String generateRawKey(EnvironmentType environmentType) {
        String prefix = environmentType == EnvironmentType.PROD ? "pk_prod_" : "pk_dev_";
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String keyMaterial = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        return prefix + keyMaterial;
    }

    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isRevoked() {
        return this.revokedAt != null;
    }

    public void recordUsage() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public ApiKeyId getId() {
        return id;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public EnvironmentId getEnvironmentId() {
        return environmentId;
    }

    public String getName() {
        return name;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
