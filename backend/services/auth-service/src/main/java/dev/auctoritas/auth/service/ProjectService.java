package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.ApiKeySecretResponse;
import dev.auctoritas.auth.api.ProjectCreateRequest;
import dev.auctoritas.auth.api.ProjectCreateResponse;
import dev.auctoritas.auth.api.ProjectSummaryResponse;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.OrganizationRepository;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
  private final OrganizationRepository organizationRepository;
  private final ProjectRepository projectRepository;
  private final ApiKeyService apiKeyService;

  public ProjectService(
      OrganizationRepository organizationRepository,
      ProjectRepository projectRepository,
      ApiKeyService apiKeyService) {
    this.organizationRepository = organizationRepository;
    this.projectRepository = projectRepository;
    this.apiKeyService = apiKeyService;
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

    Project savedProject = projectRepository.save(project);
    ApiKeyService.ApiKeySecret apiKeySecret = apiKeyService.createDefaultKey(savedProject);

    return new ProjectCreateResponse(
        toSummary(savedProject),
        new ApiKeySecretResponse(
            apiKeySecret.apiKey().getId(),
            apiKeySecret.apiKey().getName(),
            apiKeySecret.apiKey().getPrefix(),
            apiKeySecret.rawKey(),
            apiKeySecret.apiKey().getStatus(),
            apiKeySecret.apiKey().getCreatedAt()));
  }

  @Transactional(readOnly = true)
  public List<ProjectSummaryResponse> listProjects(UUID orgId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    return projectRepository.findAllByOrganizationId(orgId).stream().map(this::toSummary).toList();
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

  private String normalizeSlug(String slug) {
    return slug.trim().toLowerCase(Locale.ROOT);
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
