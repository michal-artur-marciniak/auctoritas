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
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.OrganizationRepository;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.repository.ProjectSettingsRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.common.enums.OrgMemberRole;
import dev.auctoritas.common.enums.ProjectStatus;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {
  private final OrganizationRepository organizationRepository;
  private final ProjectRepository projectRepository;
  private final ProjectSettingsRepository projectSettingsRepository;
  private final ApiKeyService apiKeyService;
  private final TextEncryptor oauthClientSecretEncryptor;

  public ProjectService(
      OrganizationRepository organizationRepository,
      ProjectRepository projectRepository,
      ProjectSettingsRepository projectSettingsRepository,
      ApiKeyService apiKeyService,
      TextEncryptor oauthClientSecretEncryptor) {
    this.organizationRepository = organizationRepository;
    this.projectRepository = projectRepository;
    this.projectSettingsRepository = projectSettingsRepository;
    this.apiKeyService = apiKeyService;
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
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
    enforceAdminAccess(principal);
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
    enforceOrgAccess(orgId, principal);
    ProjectSettings settings = loadProject(orgId, projectId).getSettings();

    Map<String, Object> merged =
        new HashMap<>(settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig());
    Map<String, Object> patch = new HashMap<>(request.config());

    // redirectUris: top-level allowlist used by OAuth flows
    if (patch.containsKey("redirectUris")) {
      merged.put("redirectUris", normalizeRedirectUris(patch.get("redirectUris")));
    }

    // google: enabled/clientId in config, clientSecret encrypted in column
    if (patch.containsKey("google")) {
      Object googleObj = patch.get("google");
      if (!(googleObj instanceof Map<?, ?> googleRaw)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_config_invalid");
      }

      Map<String, Object> existingGoogle = asObjectMap(merged.get("google"));
      Map<String, Object> google = new HashMap<>(existingGoogle);

      if (googleRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(googleRaw.get("enabled"));
        if (enabled == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_enabled_invalid");
        }
        google.put("enabled", enabled);
      }

      if (googleRaw.containsKey("clientId")) {
        String clientId = asTrimmedString(googleRaw.get("clientId"));
        if (clientId == null) {
          google.remove("clientId");
        } else {
          google.put("clientId", clientId);
        }
      }

      // Never store plaintext secrets in oauth_config.
      google.remove("clientSecret");
      google.remove("clientSecretSet");

      if (googleRaw.containsKey("clientSecret")) {
        String secret = asTrimmedStringAllowEmpty(googleRaw.get("clientSecret"));
        if (secret == null || secret.isEmpty()) {
          settings.setOauthGoogleClientSecretEnc(null);
        } else {
          settings.setOauthGoogleClientSecretEnc(oauthClientSecretEncryptor.encrypt(secret));
        }
      }

      boolean enabled = Boolean.TRUE.equals(google.get("enabled"));
      String clientId = google.get("clientId") instanceof String s ? s : null;
      boolean secretSet =
          settings.getOauthGoogleClientSecretEnc() != null
              && !settings.getOauthGoogleClientSecretEnc().trim().isEmpty();

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_google_client_id_required");
      }
      if (enabled && !secretSet) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_google_client_secret_required");
      }

      if (google.isEmpty()) {
        merged.remove("google");
      } else {
        merged.put("google", google);
      }
    }

    // github: enabled/clientId/redirectUris in config, clientSecret encrypted in column
    if (patch.containsKey("github")) {
      Object githubObj = patch.get("github");
      if (!(githubObj instanceof Map<?, ?> githubRaw)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_github_config_invalid");
      }

      Map<String, Object> existingGithub = asObjectMap(merged.get("github"));
      Map<String, Object> github = new HashMap<>(existingGithub);

      if (githubRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(githubRaw.get("enabled"));
        if (enabled == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_github_enabled_invalid");
        }
        github.put("enabled", enabled);
      }

      if (githubRaw.containsKey("clientId")) {
        String clientId = asTrimmedString(githubRaw.get("clientId"));
        if (clientId == null) {
          github.remove("clientId");
        } else {
          github.put("clientId", clientId);
        }
      }

      if (githubRaw.containsKey("redirectUris")) {
        List<String> allowlist = asStringList(merged.get("redirectUris"));
        List<String> redirectUris = normalizeRedirectUris(githubRaw.get("redirectUris"));
        for (String uri : redirectUris) {
          if (!allowlist.contains(uri)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "oauth_github_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          github.remove("redirectUris");
        } else {
          github.put("redirectUris", redirectUris);
        }
      }

      // Never store plaintext secrets in oauth_config.
      github.remove("clientSecret");
      github.remove("clientSecretSet");

      if (githubRaw.containsKey("clientSecret")) {
        String secret = asTrimmedStringAllowEmpty(githubRaw.get("clientSecret"));
        if (secret == null || secret.isEmpty()) {
          settings.setOauthGithubClientSecretEnc(null);
        } else {
          settings.setOauthGithubClientSecretEnc(oauthClientSecretEncryptor.encrypt(secret));
        }
      }

      boolean enabled = Boolean.TRUE.equals(github.get("enabled"));
      String clientId = github.get("clientId") instanceof String s ? s : null;
      boolean secretSet =
          settings.getOauthGithubClientSecretEnc() != null
              && !settings.getOauthGithubClientSecretEnc().trim().isEmpty();
      List<String> redirectUris = asStringList(github.get("redirectUris"));

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_github_client_id_required");
      }
      if (enabled && !secretSet) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_github_client_secret_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_github_redirect_uris_required");
      }

      if (github.isEmpty()) {
        merged.remove("github");
      } else {
        merged.put("github", github);
      }
    }

    // Pass through other config keys as-is.
    for (Map.Entry<String, Object> entry : patch.entrySet()) {
      String key = entry.getKey();
      if (key == null) {
        continue;
      }
      if (key.equals("google") || key.equals("github") || key.equals("redirectUris")) {
        continue;
      }
      merged.put(key, entry.getValue());
    }

    settings.setOauthConfig(merged);
    return toSettingsResponse(projectSettingsRepository.save(settings));
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

    return safe;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asObjectMap(Object value) {
    if (value instanceof Map<?, ?> map) {
      return new HashMap<>((Map<String, Object>) map);
    }
    return new HashMap<>();
  }

  private static Boolean asBoolean(Object value) {
    if (value instanceof Boolean b) {
      return b;
    }
    return null;
  }

  private static String asTrimmedString(Object value) {
    if (value == null) {
      return null;
    }
    String s = value.toString().trim();
    return s.isEmpty() ? null : s;
  }

  private static String asTrimmedStringAllowEmpty(Object value) {
    if (value == null) {
      return null;
    }
    return value.toString().trim();
  }

  private static List<String> normalizeRedirectUris(Object value) {
    if (value == null) {
      return List.of();
    }
    if (!(value instanceof List<?> list)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uris_invalid");
    }
    return list.stream()
        .map(ProjectService::asTrimmedString)
        .filter(s -> s != null)
        .map(ProjectService::validateRedirectUri)
        .distinct()
        .toList();
  }

  private static List<String> asStringList(Object value) {
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream().filter(v -> v instanceof String).map(v -> v.toString()).toList();
  }

  private static String validateRedirectUri(String raw) {
    try {
      URI uri = new URI(raw);
      String scheme = uri.getScheme();
      if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid");
      }
      if (uri.getHost() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid");
      }
      if (uri.getFragment() != null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid");
      }
      return uri.toString();
    } catch (URISyntaxException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_redirect_uri_invalid", e);
    }
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
