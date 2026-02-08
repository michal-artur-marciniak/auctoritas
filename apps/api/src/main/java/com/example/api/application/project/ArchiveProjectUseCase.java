package com.example.api.application.project;

import com.example.api.domain.project.ProjectId;
import com.example.api.domain.project.ProjectRepository;
import com.example.api.domain.project.exception.ProjectNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for archiving a project.
 */
@Component
public class ArchiveProjectUseCase {

    private final ProjectRepository projectRepository;

    public ArchiveProjectUseCase(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    public void execute(ProjectId projectId) {
        final var project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId.value()));

        project.archive();
        projectRepository.save(project);
    }
}
