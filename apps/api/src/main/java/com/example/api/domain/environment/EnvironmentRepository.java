package com.example.api.domain.environment;

import com.example.api.domain.project.ProjectId;

import java.util.List;
import java.util.Optional;

/**
 * Repository port for environment persistence.
 */
public interface EnvironmentRepository {

    List<Environment> listByProjectId(ProjectId projectId);

    Optional<Environment> findByProjectIdAndType(ProjectId projectId, EnvironmentType environmentType);

    Environment save(Environment environment);
}
