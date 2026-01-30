package dev.auctoritas.auth.application.apikey;

import dev.auctoritas.auth.api.ApiKeyCreateRequest;
import dev.auctoritas.auth.api.ApiKeySecretResponse;
import dev.auctoritas.auth.api.ApiKeySummaryResponse;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.ports.apikey.ApiKeyRepositoryPort;
import dev.auctoritas.auth.ports.security.TokenHasherPort;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.auth.service.ApiKeyService;
import dev.auctoritas.auth.domain.apikey.ApiKeyEnvironment;
import dev.auctoritas.auth.domain.organization.OrgMemberRole;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Application service that owns API key lifecycle operations. */
@Service
public class ApiKeyApplicationService {
  private static final int API_KEY_BYTES = 32;
  private static final String DEFAULT_API_KEY_NAME = "Default Key";
  private static final String LIVE_KEY_PREFIX = "pk_live_";
  private static final String TEST_KEY_PREFIX = "pk_test_";

  private final ApiKeyRepositoryPort apiKeyRepository;
  private final ProjectRepository projectRepository;
  private final ApiKeyService apiKeyService;
  private final TokenHasherPort tokenHasherPort;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

  public ApiKeyApplicationService(
      ApiKeyRepositoryPort apiKeyRepository,
      ProjectRepository projectRepository,
      ApiKeyService apiKeyService,
      TokenHasherPort tokenHasherPort) {
    this.apiKeyRepository = apiKeyRepository;
    this.projectRepository = projectRepository;
    this.apiKeyService = apiKeyService;
    this.tokenHasherPort = tokenHasherPort;
  }

  /** Creates the default API key for a newly created project. */
  @Transactional
  public ApiKeySecretResponse createDefaultKey(Project project) {
    return createKeyResponse(project, DEFAULT_API_KEY_NAME, ApiKeyEnvironment.LIVE);
  }

  /** Creates a new API key for a project. */
  @Transactional
  public ApiKeySecretResponse createApiKey(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal, ApiKeyCreateRequest request) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);
    String name = requireValue(request.name(), "api_key_name_required");
    return createKeyResponse(project, name, request.environment());
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

  private ApiKeySecretResponse createKeyResponse(
      Project project, String name, ApiKeyEnvironment environment) {
    if (apiKeyRepository.existsByProjectIdAndName(project.getId(), name)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "api_key_name_taken");
    }

    String prefix = resolvePrefix(environment);
    String rawToken = generateToken(API_KEY_BYTES);
    String rawKey = prefix + rawToken;
    ApiKey apiKey = ApiKey.create(project, name, prefix, tokenHasherPort.hashToken(rawKey));

    try {
      ApiKey savedKey = apiKeyRepository.save(apiKey);
      return toSecretResponse(savedKey, rawKey);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "api_key_name_taken", ex);
    }
  }

  private ApiKeySecretResponse toSecretResponse(ApiKey apiKey, String rawKey) {
    return new ApiKeySecretResponse(
        apiKey.getId(),
        apiKey.getName(),
        apiKey.getPrefix(),
        rawKey,
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

  private String generateToken(int length) {
    byte[] buffer = new byte[length];
    secureRandom.nextBytes(buffer);
    return encoder.encodeToString(buffer);
  }

  private String resolvePrefix(ApiKeyEnvironment environment) {
    ApiKeyEnvironment resolved = environment == null ? ApiKeyEnvironment.LIVE : environment;
    return resolved == ApiKeyEnvironment.TEST ? TEST_KEY_PREFIX : LIVE_KEY_PREFIX;
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
