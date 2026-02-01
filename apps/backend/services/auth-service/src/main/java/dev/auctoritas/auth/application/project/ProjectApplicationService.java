package dev.auctoritas.auth.application.project;

import dev.auctoritas.auth.adapter.in.web.ApiKeySecretResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectCreateRequest;
import dev.auctoritas.auth.adapter.in.web.ProjectCreateResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectSummaryResponse;
import dev.auctoritas.auth.adapter.in.web.ProjectUpdateRequest;
import dev.auctoritas.auth.application.apikey.ApiKeyApplicationService;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainForbiddenException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import dev.auctoritas.auth.domain.project.Slug;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.organization.OrganizationRepositoryPort;
import dev.auctoritas.auth.domain.project.ProjectRepositoryPort;
import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service that owns Project CRUD operations.
 * Thin orchestration layer - business logic delegated to domain entities.
 */
@Service
public class ProjectApplicationService {
  private final OrganizationRepositoryPort organizationRepository;
  private final ProjectRepositoryPort projectRepository;
  private final ApiKeyApplicationService apiKeyApplicationService;

  public ProjectApplicationService(
      OrganizationRepositoryPort organizationRepository,
      ProjectRepositoryPort projectRepository,
      ApiKeyApplicationService apiKeyApplicationService) {
    this.organizationRepository = organizationRepository;
    this.projectRepository = projectRepository;
    this.apiKeyApplicationService = apiKeyApplicationService;
  }

  @Transactional
  public ProjectCreateResponse createProject(
      UUID orgId, OrganizationMemberPrincipal principal, ProjectCreateRequest request) {
    enforceOrgAccess(orgId, principal);

    String name = requireValue(request.name(), "project_name_required");
    Slug slug = Slug.of(request.slug());

    if (projectRepository.existsBySlugAndOrganizationId(slug.value(), orgId)) {
      throw new DomainConflictException("project_slug_taken");
    }

    Organization organization =
        organizationRepository
            .findById(orgId)
            .orElseThrow(() -> new DomainNotFoundException("org_not_found"));

    try {
      Project project = Project.create(organization, name, slug);
      Project savedProject = projectRepository.save(project);
      ApiKeySecretResponse apiKeySecret = apiKeyApplicationService.createDefaultKey(savedProject);

      return new ProjectCreateResponse(toSummary(savedProject), apiKeySecret);
    } catch (DataIntegrityViolationException ex) {
      throw new DomainConflictException("project_slug_taken", ex);
    }
  }

  @Transactional(readOnly = true)
  public List<ProjectSummaryResponse> listProjects(UUID orgId, OrganizationMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    return projectRepository.findAllByOrganizationId(orgId).stream()
        .filter(project -> project.getStatus() != ProjectStatus.DELETED)
        .map(this::toSummary)
        .toList();
  }

  @Transactional
  public ProjectSummaryResponse updateProject(
      UUID orgId, UUID projectId, OrganizationMemberPrincipal principal, ProjectUpdateRequest request) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);

    if (request.name() != null) {
      project.rename(request.name());
    }

    if (request.slug() != null) {
      Slug newSlug = Slug.of(request.slug());
      if (!newSlug.value().equals(project.getSlug())
          && projectRepository.existsBySlugAndOrganizationId(newSlug.value(), orgId)) {
        throw new DomainConflictException("project_slug_taken");
      }
      project.changeSlug(newSlug);
    }

    if (request.status() != null) {
      updateProjectStatus(project, request.status());
    }

    return toSummary(projectRepository.save(project));
  }

  private void updateProjectStatus(Project project, ProjectStatus requestedStatus) {
    switch (requestedStatus) {
      case ACTIVE -> project.reactivate();
      case ARCHIVED -> project.archive();
      case SUSPENDED -> project.suspend();
      case DELETED -> throw new DomainConflictException("project_status_invalid");
    }
  }

  @Transactional
  public void deleteProject(UUID orgId, UUID projectId, OrganizationMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    Project project = loadProject(orgId, projectId);
    project.markDeleted();
    projectRepository.save(project);
    apiKeyApplicationService.revokeAllByProjectId(projectId);
  }

  private ProjectSummaryResponse toSummary(Project project) {
    return new ProjectSummaryResponse(
        project.getId(),
        project.getName(),
        project.getSlug(),
        project.getStatus(),
        project.getCreatedAt(),
        project.getUpdatedAt());
  }

  private void enforceOrgAccess(UUID orgId, OrganizationMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    if (!orgId.equals(principal.orgId())) {
      throw new DomainForbiddenException("org_access_denied");
    }
  }

  private void enforceAdminAccess(OrganizationMemberPrincipal principal) {
    OrganizationMemberRole role = principal.role();
    if (role != OrganizationMemberRole.OWNER && role != OrganizationMemberRole.ADMIN) {
      throw new DomainForbiddenException("insufficient_role");
    }
  }

  private Project loadProject(UUID orgId, UUID projectId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new DomainNotFoundException("project_not_found"));
    if (!orgId.equals(project.getOrganization().getId())) {
      throw new DomainNotFoundException("project_not_found");
    }
    return project;
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
