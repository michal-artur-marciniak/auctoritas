package com.example.api.domain.environment;

import com.example.api.domain.project.ProjectId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing a project environment.
 */
public class Environment {

    private final EnvironmentId id;
    private final ProjectId projectId;
    private final EnvironmentType environmentType;
    private final LocalDateTime createdAt;

    public Environment(EnvironmentId id,
                       ProjectId projectId,
                       EnvironmentType environmentType,
                       LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "Environment ID required");
        this.projectId = Objects.requireNonNull(projectId, "Project ID required");
        this.environmentType = Objects.requireNonNull(environmentType, "Environment type required");
        this.createdAt = Objects.requireNonNull(createdAt, "Created timestamp required");
    }

    public static Environment create(ProjectId projectId, EnvironmentType environmentType) {
        return new Environment(
                EnvironmentId.generate(),
                projectId,
                environmentType,
                LocalDateTime.now()
        );
    }

    public EnvironmentId getId() {
        return id;
    }

    public ProjectId getProjectId() {
        return projectId;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
