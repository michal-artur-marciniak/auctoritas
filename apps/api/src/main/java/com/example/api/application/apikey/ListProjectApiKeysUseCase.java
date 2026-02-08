package com.example.api.application.apikey;

import com.example.api.application.apikey.dto.ApiKeyResponse;
import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyRepository;
import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.ProjectId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Use case for listing API keys for a project.
 */
@Component
public class ListProjectApiKeysUseCase {

    private final ApiKeyRepository apiKeyRepository;
    private final EnvironmentRepository environmentRepository;

    public ListProjectApiKeysUseCase(ApiKeyRepository apiKeyRepository,
                                     EnvironmentRepository environmentRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.environmentRepository = environmentRepository;
    }

    public List<ApiKeyResponse> execute(ProjectId projectId) {
        final var environments = environmentRepository.listByProjectId(projectId);
        final var environmentTypeMap = environments.stream()
            .collect(Collectors.toMap(
                env -> env.getId().value(),
                Environment::getEnvironmentType
            ));

        final var apiKeys = apiKeyRepository.listByProjectId(projectId);

        return apiKeys.stream()
            .map(apiKey -> {
                final var envType = environmentTypeMap.getOrDefault(
                    apiKey.getEnvironmentId().value(),
                    EnvironmentType.DEV
                );
                return ApiKeyResponse.from(apiKey, envType);
            })
            .toList();
    }
}
