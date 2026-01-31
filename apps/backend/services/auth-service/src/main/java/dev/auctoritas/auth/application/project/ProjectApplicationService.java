package dev.auctoritas.auth.application.project;

import dev.auctoritas.auth.api.ApiKeySecretResponse;
import dev.auctoritas.auth.api.ProjectCreateRequest;
import dev.auctoritas.auth.api.ProjectCreateResponse;
import dev.auctoritas.auth.api.ProjectSummaryResponse;
import dev.auctoritas.auth.api.ProjectUpdateRequest;
import dev.auctoritas.auth.application.apikey.ApiKeyApplicationService;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.ports.organization.OrganizationRepositoryPort;
import dev.auctoritas.auth.ports.project.ProjectRepositoryPort;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.auth.domain.organization.OrgMemberRole;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Application service that owns Project CRUD operations. */
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
      UUID orgId, OrgMemberPrincipal principal, ProjectCreateRequest request) {
    enforceOrgAccess(orgId, principal);

    String name = requireValue(request.name(), "project_name_required");
    String slug = normalizeSlug(requireValue(request.slug(), "project_slug_required"));

    if (projectRepository.existsBySlugAndOrganizationId(slug, orgId)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "project_slug_taken");
    }

    Organization organization =
        organizationRepository
            .findById(orgId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "org_not_found"));

    Project project = new Project();
    project.setOrganization(organization);
    project.setName(name);
    project.setSlug(slug);

    ProjectSettings settings = new ProjectSettings();
    settings.setProject(project);
    project.setSettings(settings);

    try {
      Project savedProject = projectRepository.save(project);
      ApiKeySecretResponse apiKeySecret = apiKeyApplicationService.createDefaultKey(savedProject);

      return new ProjectCreateResponse(
          toSummary(savedProject),
          apiKeySecret);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "project_slug_taken", ex);
    }
  }

  @Transactional(readOnly = true)
  public List<ProjectSummaryResponse> listProjects(UUID orgId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    return projectRepository.findAllByOrganizationId(orgId).stream()
        .filter(project -> project.getStatus() != ProjectStatus.DELETED)
        .map(this::toSummary)
        .toList();
  }

  @Transactional
  public ProjectSummaryResponse updateProject(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal, ProjectUpdateRequest request) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);
    if (project.getStatus() == ProjectStatus.DELETED) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found");
    }

    if (request.name() != null) {
      project.setName(requireValue(request.name(), "project_name_required"));
    }

    if (request.slug() != null) {
      String normalized = normalizeSlug(requireValue(request.slug(), "project_slug_required"));
      if (!normalized.equals(project.getSlug())
          && projectRepository.existsBySlugAndOrganizationId(normalized, orgId)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "project_slug_taken");
      }
      project.setSlug(normalized);
    }

    if (request.status() != null) {
      ProjectStatus requestedStatus = request.status();
      if (requestedStatus == ProjectStatus.DELETED) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "project_status_invalid");
      }
      project.setStatus(requestedStatus);
    }

    return toSummary(projectRepository.save(project));
  }

  @Transactional
  public void deleteProject(UUID orgId, UUID projectId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    Project project = loadProject(orgId, projectId);
    project.setStatus(ProjectStatus.DELETED);
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

  private void enforceOrgAccess(UUID orgId, OrgMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    if (!orgId.equals(principal.orgId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_access_denied");
    }
  }

  private void enforceAdminAccess(OrgMemberPrincipal principal) {
    OrgMemberRole role = principal.role();
    if (role != OrgMemberRole.OWNER && role != OrgMemberRole.ADMIN) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "insufficient_role");
    }
  }

  private String normalizeSlug(String slug) {
    return slug.trim().toLowerCase(Locale.ROOT);
  }

  private Project loadProject(UUID orgId, UUID projectId) {
    Project project =
        projectRepository
            .findById(projectId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found"));
    if (!orgId.equals(project.getOrganization().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "project_not_found");
    }
    return project;
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    return trimmed;
  }
}
