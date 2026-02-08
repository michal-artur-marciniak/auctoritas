package com.example.api.application.project;

import com.example.api.application.project.dto.ProjectResponse;
import com.example.api.domain.environment.EnvironmentRepository;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.project.Project;
import com.example.api.domain.project.ProjectRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Use case for listing projects in an organization.
 */
@Component
public class ListOrganizationProjectsUseCase {

    private final ProjectRepository projectRepository;
    private final EnvironmentRepository environmentRepository;

    public ListOrganizationProjectsUseCase(ProjectRepository projectRepository,
                                           EnvironmentRepository environmentRepository) {
        this.projectRepository = projectRepository;
        this.environmentRepository = environmentRepository;
    }

    public List<ProjectResponse> execute(OrganizationId organizationId) {
        final var projects = projectRepository.listByOrganizationId(organizationId);

        return projects.stream()
                .map(project -> {
                    final var environments = environmentRepository.listByProjectId(project.getId());
                    return ProjectResponse.from(project, environments);
                })
                .toList();
    }
}
