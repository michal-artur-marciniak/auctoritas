package com.example.api.infrastructure.security;

import com.example.api.domain.environment.EnvironmentId;
import com.example.api.domain.project.ProjectId;

import java.util.Objects;

/**
 * Immutable holder for project and environment context resolved from API key.
 * Stored in request attributes for the duration of the request.
 */
public final class ProjectContext {

    private final ProjectId projectId;
    private final EnvironmentId environmentId;

    public ProjectContext(ProjectId projectId, EnvironmentId environmentId) {
        this.projectId = Objects.requireNonNull(projectId, "Project ID required");
        this.environmentId = Objects.requireNonNull(environmentId, "Environment ID required");
    }

    public ProjectId projectId() {
        return projectId;
    }

    public EnvironmentId environmentId() {
        return environmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectContext that = (ProjectContext) o;
        return Objects.equals(projectId, that.projectId) &&
                Objects.equals(environmentId, that.environmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, environmentId);
    }
}
