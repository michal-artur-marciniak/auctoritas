package com.example.api.application.project;

import com.example.api.application.project.dto.CreateProjectRequest;
import com.example.api.application.project.dto.ProjectResponse;
import com.example.api.domain.apikey.ApiKey;
import com.example.api.domain.apikey.ApiKeyRepository;
import com.example.api.domain.environment.Environment;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.environment.EnvironmentType;
import com.example.api.domain.project.Project;
import com.example.api.domain.project.ProjectRepository;
import com.example.api.domain.project.exception.ProjectSlugAlreadyExistsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Use case for creating a project with PROD and DEV environments and API keys.
 */
@Component
public class CreateProjectUseCase {

    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;
    private final ApiKeyRepository apiKeyRepository;

    public CreateProjectUseCase(ProjectRepository projectRepository,
                                EnvironmentRepository environmentRepository,
                                ApiKeyRepository apiKeyRepository) {
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Transactional
    public ProjectResponse execute(CreateProjectRequest request) {
        if (projectRepository.findBySlugAndOrganizationId(request.slug(), request.organizationId()).isPresent()) {
            throw new ProjectSlugAlreadyExistsException(request.slug());
        }

        final var project = Project.create(
                request.organizationId(),
                request.name(),
                request.slug(),
                request.description()
        );

        projectRepository.save(project);

        final var prodEnvironment = Environment.create(project.getId(), EnvironmentType.PROD);
        final var devEnvironment = Environment.create(project.getId(), EnvironmentType.DEV);

        environmentRepository.save(prodEnvironment);
        environmentRepository.save(devEnvironment);

        final var environments = List.of(prodEnvironment, devEnvironment);

        final var prodRawKey = ApiKey.generateRawKey(EnvironmentType.PROD);
        final var devRawKey = ApiKey.generateRawKey(EnvironmentType.DEV);

        final var prodKeyHash = hashKey(prodRawKey);
        final var devKeyHash = hashKey(devRawKey);

        final var prodApiKey = ApiKey.create(
                project.getId(),
                prodEnvironment.getId(),
                EnvironmentType.PROD,
                "Production API Key",
                prodKeyHash
        );

        final var devApiKey = ApiKey.create(
                project.getId(),
                devEnvironment.getId(),
                EnvironmentType.DEV,
                "Development API Key",
                devKeyHash
        );

        apiKeyRepository.save(prodApiKey);
        apiKeyRepository.save(devApiKey);

        final var apiKeys = List.of(
                new ProjectResponse.ApiKeyWithType(prodApiKey, EnvironmentType.PROD, prodRawKey),
                new ProjectResponse.ApiKeyWithType(devApiKey, EnvironmentType.DEV, devRawKey)
        );

        return ProjectResponse.from(project, environments, apiKeys);
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
