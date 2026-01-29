package dev.auctoritas.auth.application.apikey;

import dev.auctoritas.auth.api.ApiKeyCreateRequest;
import dev.auctoritas.auth.api.ApiKeySecretResponse;
import dev.auctoritas.auth.api.ApiKeySummaryResponse;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.auth.service.ApiKeyService;
import dev.auctoritas.auth.shared.enums.OrgMemberRole;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Application service that owns API key lifecycle operations. */
@Service
public class ApiKeyApplicationService {
  private final ProjectRepository projectRepository;
  private final ApiKeyService apiKeyService;

  public ApiKeyApplicationService(ProjectRepository projectRepository, ApiKeyService apiKeyService) {
    this.projectRepository = projectRepository;
    this.apiKeyService = apiKeyService;
  }

  /** Creates the default API key for a newly created project. */
  @Transactional
  public ApiKeySecretResponse createDefaultKey(Project project) {
    return toSecretResponse(apiKeyService.createDefaultKey(project));
  }

  /** Creates a new API key for a project. */
  @Transactional
  public ApiKeySecretResponse createApiKey(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal, ApiKeyCreateRequest request) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);
    String name = requireValue(request.name(), "api_key_name_required");
    ApiKeyService.ApiKeySecret apiKeySecret =
        apiKeyService.createKey(project, name, request.environment());
    return toSecretResponse(apiKeySecret);
  }

  /** Lists API keys for a project. */
  @Transactional(readOnly = true)
  public List<ApiKeySummaryResponse> listApiKeys(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    loadProject(orgId, projectId);
    return apiKeyService.listKeys(projectId).stream().map(this::toApiKeySummary).toList();
  }

  /** Revokes a project API key. */
  @Transactional
  public void revokeApiKey(UUID orgId, UUID projectId, UUID keyId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    loadProject(orgId, projectId);
    apiKeyService.revokeKey(projectId, keyId);
  }

  /** Revokes all API keys for a project. */
  @Transactional
  public void revokeAllByProjectId(UUID projectId) {
    apiKeyService.revokeAllByProjectId(projectId);
  }

  private ApiKeySecretResponse toSecretResponse(ApiKeyService.ApiKeySecret apiKeySecret) {
    ApiKey apiKey = apiKeySecret.apiKey();
    return new ApiKeySecretResponse(
        apiKey.getId(),
        apiKey.getName(),
        apiKey.getPrefix(),
        apiKeySecret.rawKey(),
        apiKey.getStatus(),
        apiKey.getCreatedAt());
  }

  private ApiKeySummaryResponse toApiKeySummary(ApiKey apiKey) {
    return new ApiKeySummaryResponse(
        apiKey.getId(),
        apiKey.getName(),
        apiKey.getPrefix(),
        apiKey.getStatus(),
        apiKey.getLastUsedAt(),
        apiKey.getCreatedAt());
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
