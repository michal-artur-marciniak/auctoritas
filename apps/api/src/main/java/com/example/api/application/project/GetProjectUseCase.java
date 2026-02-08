package com.example.api.application.project;

import com.example.api.application.project.dto.ProjectResponse;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.project.ProjectId;
import com.example.api.domain.project.ProjectRepository;
import com.example.api.domain.project.exception.ProjectNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Use case for getting a single project with environments.
 */
@Component
public class GetProjectUseCase {

    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;

    public GetProjectUseCase(ProjectRepository projectRepository,
                             EnvironmentRepository environmentRepository) {
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
    }

    public ProjectResponse execute(ProjectId projectId) {
        final var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId.value()));

        final var environments = environmentRepository.listByProjectId(projectId);

        return ProjectResponse.from(project, environments);
    }
}
