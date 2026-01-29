package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.ApiKeyCreateRequest;
import dev.auctoritas.auth.api.ApiKeySecretResponse;
import dev.auctoritas.auth.api.ApiKeySummaryResponse;
import dev.auctoritas.auth.api.ProjectCreateRequest;
import dev.auctoritas.auth.api.ProjectCreateResponse;
import dev.auctoritas.auth.api.ProjectAuthSettingsRequest;
import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.api.ProjectPasswordSettingsRequest;
import dev.auctoritas.auth.api.ProjectSessionSettingsRequest;
import dev.auctoritas.auth.api.ProjectSettingsResponse;
import dev.auctoritas.auth.api.ProjectSummaryResponse;
import dev.auctoritas.auth.api.ProjectUpdateRequest;
import dev.auctoritas.auth.application.project.ProjectApplicationService;
import dev.auctoritas.auth.application.project.ProjectOAuthSettingsApplicationService;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.repository.ProjectSettingsRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.auth.shared.enums.OrgMemberRole;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
  private final ProjectRepository projectRepository;
  private final ProjectSettingsRepository projectSettingsRepository;
  private final ProjectApplicationService projectApplicationService;
  private final ProjectOAuthSettingsApplicationService projectOAuthSettingsApplicationService;
  private final ApiKeyService apiKeyService;

  public ProjectService(
      ProjectRepository projectRepository,
      ProjectSettingsRepository projectSettingsRepository,
      ProjectApplicationService projectApplicationService,
      ProjectOAuthSettingsApplicationService projectOAuthSettingsApplicationService,
      ApiKeyService apiKeyService) {
    this.projectRepository = projectRepository;
    this.projectSettingsRepository = projectSettingsRepository;
    this.projectApplicationService = projectApplicationService;
    this.projectOAuthSettingsApplicationService = projectOAuthSettingsApplicationService;
    this.apiKeyService = apiKeyService;
  }

  @Transactional
  public ProjectCreateResponse createProject(
      UUID orgId, OrgMemberPrincipal principal, ProjectCreateRequest request) {
    return projectApplicationService.createProject(orgId, principal, request);
  }

  @Transactional(readOnly = true)
  public List<ProjectSummaryResponse> listProjects(UUID orgId, OrgMemberPrincipal principal) {
    return projectApplicationService.listProjects(orgId, principal);
  }

  @Transactional
  public ProjectSummaryResponse updateProject(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal, ProjectUpdateRequest request) {
    return projectApplicationService.updateProject(orgId, projectId, principal, request);
  }

  @Transactional
  public void deleteProject(UUID orgId, UUID projectId, OrgMemberPrincipal principal) {
    projectApplicationService.deleteProject(orgId, projectId, principal);
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
    settings.setRequireLowercase(request.requireLowercase());
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
  public ProjectSettingsResponse updateAuthSettings(
      UUID orgId,
      UUID projectId,
      OrgMemberPrincipal principal,
      ProjectAuthSettingsRequest request) {
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();
    settings.setRequireVerifiedEmailForLogin(request.requireVerifiedEmailForLogin());
    return toSettingsResponse(projectSettingsRepository.save(settings));
  }

  @Transactional
  public ProjectSettingsResponse updateOAuthSettings(
      UUID orgId,
      UUID projectId,
      OrgMemberPrincipal principal,
      ProjectOAuthSettingsRequest request) {
    return toSettingsResponse(
        projectOAuthSettingsApplicationService.updateOAuthSettings(
            orgId, projectId, principal, request));
  }

  @Transactional
  public ApiKeySecretResponse createApiKey(
      UUID orgId,
      UUID projectId,
      OrgMemberPrincipal principal,
      ApiKeyCreateRequest request) {
    enforceOrgAccess(orgId, principal);
    Project project = loadProject(orgId, projectId);
    String name = requireValue(request.name(), "api_key_name_required");
    ApiKeyService.ApiKeySecret apiKeySecret = apiKeyService.createKey(project, name, request.environment());
    return new ApiKeySecretResponse(
        apiKeySecret.apiKey().getId(),
        apiKeySecret.apiKey().getName(),
        apiKeySecret.apiKey().getPrefix(),
        apiKeySecret.rawKey(),
        apiKeySecret.apiKey().getStatus(),
        apiKeySecret.apiKey().getCreatedAt());
  }

  @Transactional(readOnly = true)
  public List<ApiKeySummaryResponse> listApiKeys(
      UUID orgId, UUID projectId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    loadProject(orgId, projectId);
    return apiKeyService.listKeys(projectId).stream().map(this::toApiKeySummary).toList();
  }

  @Transactional
  public void revokeApiKey(
      UUID orgId, UUID projectId, UUID keyId, OrgMemberPrincipal principal) {
    enforceOrgAccess(orgId, principal);
    enforceAdminAccess(principal);
    loadProject(orgId, projectId);
    apiKeyService.revokeKey(projectId, keyId);
  }

  private ProjectSettingsResponse toSettingsResponse(ProjectSettings settings) {
    return new ProjectSettingsResponse(
        settings.getMinLength(),
        settings.isRequireUppercase(),
        settings.isRequireLowercase(),
        settings.isRequireNumbers(),
        settings.isRequireSpecialChars(),
        settings.getPasswordHistoryCount(),
        settings.getAccessTokenTtlSeconds(),
        settings.getRefreshTokenTtlSeconds(),
        settings.getMaxSessions(),
        settings.isRequireVerifiedEmailForLogin(),
        settings.isMfaEnabled(),
        settings.isMfaRequired(),
        toSafeOauthConfig(settings));
  }

  private Map<String, Object> toSafeOauthConfig(ProjectSettings settings) {
    Map<String, Object> stored = settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig();
    Map<String, Object> safe = new HashMap<>(stored);

    boolean hadGoogle = stored.containsKey("google");
    Map<String, Object> google = asObjectMap(safe.get("google"));
    google.remove("clientSecret");
    google.remove("clientSecretSet");

    boolean secretSet =
        settings.getOauthGoogleClientSecretEnc() != null
            && !settings.getOauthGoogleClientSecretEnc().trim().isEmpty();
    if (hadGoogle || secretSet) {
      google.put("clientSecretSet", secretSet);
      if (secretSet) {
        google.put("clientSecret", "********");
      }
      safe.put("google", google);
    }

    boolean hadGithub = stored.containsKey("github");
    Map<String, Object> github = asObjectMap(safe.get("github"));
    github.remove("clientSecret");
    github.remove("clientSecretSet");

    boolean githubSecretSet =
        settings.getOauthGithubClientSecretEnc() != null
            && !settings.getOauthGithubClientSecretEnc().trim().isEmpty();
    if (hadGithub || githubSecretSet) {
      github.put("clientSecretSet", githubSecretSet);
      if (githubSecretSet) {
        github.put("clientSecret", "********");
      }
      safe.put("github", github);
    }

    boolean hadMicrosoft = stored.containsKey("microsoft");
    Map<String, Object> microsoft = asObjectMap(safe.get("microsoft"));
    microsoft.remove("clientSecret");
    microsoft.remove("clientSecretSet");

    boolean microsoftSecretSet =
        settings.getOauthMicrosoftClientSecretEnc() != null
            && !settings.getOauthMicrosoftClientSecretEnc().trim().isEmpty();
    if (hadMicrosoft || microsoftSecretSet) {
      microsoft.put("clientSecretSet", microsoftSecretSet);
      if (microsoftSecretSet) {
        microsoft.put("clientSecret", "********");
      }
      safe.put("microsoft", microsoft);
    }

    boolean hadFacebook = stored.containsKey("facebook");
    Map<String, Object> facebook = asObjectMap(safe.get("facebook"));
    facebook.remove("clientSecret");
    facebook.remove("clientSecretSet");

    boolean facebookSecretSet =
        settings.getOauthFacebookClientSecretEnc() != null
            && !settings.getOauthFacebookClientSecretEnc().trim().isEmpty();
    if (hadFacebook || facebookSecretSet) {
      facebook.put("clientSecretSet", facebookSecretSet);
      if (facebookSecretSet) {
        facebook.put("clientSecret", "********");
      }
      safe.put("facebook", facebook);
    }

    boolean hadApple = stored.containsKey("apple");
    Map<String, Object> apple = asObjectMap(safe.get("apple"));
    apple.remove("privateKey");
    apple.remove("privateKeySet");

    boolean appleKeySet =
        settings.getOauthApplePrivateKeyEnc() != null
            && !settings.getOauthApplePrivateKeyEnc().trim().isEmpty();
    if (hadApple || appleKeySet) {
      apple.put("privateKeySet", appleKeySet);
      if (appleKeySet) {
        apple.put("privateKey", "********");
      }
      safe.put("apple", apple);
    }

    return safe;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asObjectMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return new HashMap<>((Map<String, Object>) map);
    }
    return new HashMap<>();
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
