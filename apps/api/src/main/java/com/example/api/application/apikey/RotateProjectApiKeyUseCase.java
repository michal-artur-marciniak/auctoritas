package com.example.api.application.apikey;

import com.example.api.application.apikey.dto.RotateApiKeyRequest;
import com.example.api.application.apikey.dto.RotatedApiKeyResponse;
import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyRepository;
import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.ProjectId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Use case for rotating an API key.
 */
@Component
public class RotateProjectApiKeyUseCase {

    private final ApiKeyRepository apiKeyRepository;
    private final EnvironmentRepository environmentRepository;

    public RotateProjectApiKeyUseCase(ApiKeyRepository apiKeyRepository,
                                      EnvironmentRepository environmentRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional
    public RotatedApiKeyResponse execute(ProjectId projectId, RotateApiKeyRequest request) {
        final var environment = environmentRepository
            .findByProjectIdAndType(projectId, EnvironmentType.valueOf(request.environmentId()))
            .orElseThrow(() -> new IllegalArgumentException("Environment not found for project"));

        final var existingKeys = apiKeyRepository.listByProjectId(projectId);
        final var keyToRevoke = existingKeys.stream()
            .filter(key -> key.getEnvironmentId().equals(environment.getId()))
            .filter(key -> !key.isRevoked())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No active API key found for environment"));

        apiKeyRepository.revoke(keyToRevoke.getId());

        final var rawKey = ApiKey.generateRawKey(environment.getEnvironmentType());
        final var keyHash = hashKey(rawKey);

        final var newApiKey = ApiKey.create(
            projectId,
            environment.getId(),
            environment.getEnvironmentType(),
            keyToRevoke.getName(),
            keyHash
        );

        apiKeyRepository.save(newApiKey);

        return RotatedApiKeyResponse.from(newApiKey, environment.getEnvironmentType(), rawKey);
    }

    private String hashKey(String rawKey) {
        try {
            final var digest = MessageDigest.getInstance("SHA-256");
            final var hash = digest.digest(rawKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
