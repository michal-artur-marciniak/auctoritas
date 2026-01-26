package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.ApiKeySecretResponse;
import dev.auctoritas.auth.api.ProjectCreateRequest;
import dev.auctoritas.auth.api.ProjectCreateResponse;
import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.api.ProjectPasswordSettingsRequest;
import dev.auctoritas.auth.api.ProjectSessionSettingsRequest;
import dev.auctoritas.auth.api.ProjectSettingsResponse;
import dev.auctoritas.auth.api.ProjectSummaryResponse;
import dev.auctoritas.auth.api.ProjectUpdateRequest;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.OrganizationRepository;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.repository.ProjectSettingsRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.common.enums.ProjectStatus;
import java.util.HashMap;
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
  private final ProjectSettingsRepository projectSettingsRepository;
  private final ApiKeyService apiKeyService;

  public ProjectService(
      OrganizationRepository organizationRepository,
      ProjectRepository projectRepository,
      ProjectSettingsRepository projectSettingsRepository,
      ApiKeyService apiKeyService) {
    this.organizationRepository = organizationRepository;
    this.projectRepository = projectRepository;
    this.projectSettingsRepository = projectSettingsRepository;
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

  @Transactional
  public ProjectSummaryResponse updateProject(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal, ProjectUpdateRequest request) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);

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
      project.setStatus(request.status());
    }

    return toSummary(projectRepository.save(project));
  }

  @Transactional
  public void deleteProject(UUID orgId, UUID projectId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);
    project.setStatus(ProjectStatus.DELETED);
    projectRepository.save(project);
    apiKeyService.revokeAllByProjectId(projectId);
  }

  @Transactional(readOnly = true)
  public ProjectSettingsResponse getProjectSettings(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);
    return toSettingsResponse(project.getSettings());
  }

  @Transactional
  public ProjectSettingsResponse updatePasswordSettings(
      UUID orgId,
      UUID projectId,
      OrgMemberPrincipal principal,
      ProjectPasswordSettingsRequest request) {
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();
    settings.setMinLength(request.minLength());
    settings.setRequireUppercase(request.requireUppercase());
    settings.setRequireNumbers(request.requireNumbers());
    settings.setRequireSpecialChars(request.requireSpecialChars());
    settings.setPasswordHistoryCount(request.passwordHistoryCount());
    return toSettingsResponse(projectSettingsRepository.save(settings));
  }

  @Transactional
  public ProjectSettingsResponse updateSessionSettings(
      UUID orgId,
      UUID projectId,
      OrgMemberPrincipal principal,
      ProjectSessionSettingsRequest request) {
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();
    settings.setAccessTokenTtlSeconds(request.accessTokenTtlSeconds());
    settings.setRefreshTokenTtlSeconds(request.refreshTokenTtlSeconds());
    settings.setMaxSessions(request.maxSessions());
    settings.setMfaEnabled(request.mfaEnabled());
    settings.setMfaRequired(request.mfaRequired());
    return toSettingsResponse(projectSettingsRepository.save(settings));
  }

  @Transactional
  public ProjectSettingsResponse updateOAuthSettings(
      UUID orgId,
      UUID projectId,
      OrgMemberPrincipal principal,
      ProjectOAuthSettingsRequest request) {
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();
    settings.setOauthConfig(new HashMap<>(request.config()));
    return toSettingsResponse(projectSettingsRepository.save(settings));
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

  private ProjectSettingsResponse toSettingsResponse(ProjectSettings settings) {
    return new ProjectSettingsResponse(
        settings.getMinLength(),
        settings.isRequireUppercase(),
        settings.isRequireNumbers(),
        settings.isRequireSpecialChars(),
        settings.getPasswordHistoryCount(),
        settings.getAccessTokenTtlSeconds(),
        settings.getRefreshTokenTtlSeconds(),
        settings.getMaxSessions(),
        settings.isMfaEnabled(),
        settings.isMfaRequired(),
        settings.getOauthConfig());
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
