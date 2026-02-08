package com.example.api.presentation.project;

import com.example.api.application.project.ArchiveProjectUseCase;
import com.example.api.application.project.CreateProjectUseCase;
import com.example.api.application.project.GetProjectUseCase;
import com.example.api.application.project.ListOrganizationProjectsUseCase;
import com.example.api.application.project.dto.ProjectResponse;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.project.ProjectId;
import com.example.api.presentation.project.dto.CreateProjectRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for project management.
 */
@RestController
@RequestMapping("/api/v1/org/{orgId}/projects")
public class ProjectController {

    private final CreateProjectUseCase createProjectUseCase;
    private final ListOrganizationProjectsUseCase listOrganizationProjectsUseCase;
    private final GetProjectUseCase getProjectUseCase;
    private final ArchiveProjectUseCase archiveProjectUseCase;

    public ProjectController(CreateProjectUseCase createProjectUseCase,
                             ListOrganizationProjectsUseCase listOrganizationProjectsUseCase,
                             GetProjectUseCase getProjectUseCase,
                             ArchiveProjectUseCase archiveProjectUseCase) {
        this.createProjectUseCase = createProjectUseCase;
        this.listOrganizationProjectsUseCase = listOrganizationProjectsUseCase;
        this.getProjectUseCase = getProjectUseCase;
        this.archiveProjectUseCase = archiveProjectUseCase;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable String orgId,
            @Valid @RequestBody CreateProjectRequestDto dto) {
        final var organizationId = OrganizationId.of(orgId);
        final var response = createProjectUseCase.execute(dto.toRequest(organizationId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listProjects(@PathVariable String orgId) {
        final var organizationId = OrganizationId.of(orgId);
        final var projects = listOrganizationProjectsUseCase.execute(organizationId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(
            @PathVariable String orgId,
            @PathVariable String projectId) {
        final var response = getProjectUseCase.execute(ProjectId.of(projectId));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> archiveProject(
            @PathVariable String orgId,
            @PathVariable String projectId) {
        archiveProjectUseCase.execute(ProjectId.of(projectId));
        return ResponseEntity.noContent().build();
    }
}
