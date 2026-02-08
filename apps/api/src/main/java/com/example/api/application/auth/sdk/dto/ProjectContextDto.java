package com.example.api.application.auth.sdk.dto;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;

import java.util.Objects;

/**
 * Result DTO containing resolved project context from API key.
 */
public record ProjectContextDto(ProjectId projectId, EnvironmentId environmentId) {

    public ProjectContextDto {
        Objects.requireNonNull(projectId, "Project ID required");
        Objects.requireNonNull(environmentId, "Environment ID required");
    }
}
