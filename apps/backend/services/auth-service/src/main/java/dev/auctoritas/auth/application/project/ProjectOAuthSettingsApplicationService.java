package dev.auctoritas.auth.application.project;

import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.repository.ProjectSettingsRepository;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Application service that owns Project OAuth settings updates. */
@Service
public class ProjectOAuthSettingsApplicationService {
  private final ProjectRepository projectRepository;
  private final ProjectSettingsRepository projectSettingsRepository;
  private final TextEncryptor oauthClientSecretEncryptor;

  public ProjectOAuthSettingsApplicationService(
      ProjectRepository projectRepository,
      ProjectSettingsRepository projectSettingsRepository,
      TextEncryptor oauthClientSecretEncryptor) {
    this.projectRepository = projectRepository;
    this.projectSettingsRepository = projectSettingsRepository;
    this.oauthClientSecretEncryptor = oauthClientSecretEncryptor;
  }

  /** Updates OAuth settings for a project and persists encrypted secrets. */
  @Transactional
  public ProjectSettings updateOAuthSettings(
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

    // microsoft: enabled/clientId/tenant/redirectUris in config, clientSecret encrypted in column
    if (patch.containsKey("microsoft")) {
      Object microsoftObj = patch.get("microsoft");
      if (!(microsoftObj instanceof Map<?, ?> microsoftRaw)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_microsoft_config_invalid");
      }

      Map<String, Object> existingMicrosoft = asObjectMap(merged.get("microsoft"));
      Map<String, Object> microsoft = new HashMap<>(existingMicrosoft);

      if (microsoftRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(microsoftRaw.get("enabled"));
        if (enabled == null) {
          throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST, "oauth_microsoft_enabled_invalid");
        }
        microsoft.put("enabled", enabled);
      }

      if (microsoftRaw.containsKey("clientId")) {
        String clientId = asTrimmedString(microsoftRaw.get("clientId"));
        if (clientId == null) {
          microsoft.remove("clientId");
        } else {
          microsoft.put("clientId", clientId);
        }
      }

      if (microsoftRaw.containsKey("tenant")) {
        String tenant = asTrimmedString(microsoftRaw.get("tenant"));
        if (tenant == null) {
          microsoft.remove("tenant");
        } else {
          microsoft.put("tenant", tenant);
        }
      }

      if (microsoftRaw.containsKey("redirectUris")) {
        List<String> allowlist = asStringList(merged.get("redirectUris"));
        List<String> redirectUris = normalizeRedirectUris(microsoftRaw.get("redirectUris"));
        for (String uri : redirectUris) {
          if (!allowlist.contains(uri)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "oauth_microsoft_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          microsoft.remove("redirectUris");
        } else {
          microsoft.put("redirectUris", redirectUris);
        }
      }

      // Never store plaintext secrets in oauth_config.
      microsoft.remove("clientSecret");
      microsoft.remove("clientSecretSet");

      if (microsoftRaw.containsKey("clientSecret")) {
        String secret = asTrimmedStringAllowEmpty(microsoftRaw.get("clientSecret"));
        if (secret == null || secret.isEmpty()) {
          settings.setOauthMicrosoftClientSecretEnc(null);
        } else {
          settings.setOauthMicrosoftClientSecretEnc(oauthClientSecretEncryptor.encrypt(secret));
        }
      }

      boolean enabled = Boolean.TRUE.equals(microsoft.get("enabled"));
      String clientId = microsoft.get("clientId") instanceof String s ? s : null;
      boolean secretSet =
          settings.getOauthMicrosoftClientSecretEnc() != null
              && !settings.getOauthMicrosoftClientSecretEnc().trim().isEmpty();
      List<String> redirectUris = asStringList(microsoft.get("redirectUris"));

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_microsoft_client_id_required");
      }
      if (enabled && !secretSet) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_microsoft_client_secret_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_microsoft_redirect_uris_required");
      }

      if (microsoft.isEmpty()) {
        merged.remove("microsoft");
      } else {
        merged.put("microsoft", microsoft);
      }
    }

    // facebook: enabled/clientId/redirectUris in config, clientSecret encrypted in column
    if (patch.containsKey("facebook")) {
      Object facebookObj = patch.get("facebook");
      if (!(facebookObj instanceof Map<?, ?> facebookRaw)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_facebook_config_invalid");
      }

      Map<String, Object> existingFacebook = asObjectMap(merged.get("facebook"));
      Map<String, Object> facebook = new HashMap<>(existingFacebook);

      if (facebookRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(facebookRaw.get("enabled"));
        if (enabled == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_facebook_enabled_invalid");
        }
        facebook.put("enabled", enabled);
      }

      if (facebookRaw.containsKey("clientId")) {
        String clientId = asTrimmedString(facebookRaw.get("clientId"));
        if (clientId == null) {
          facebook.remove("clientId");
        } else {
          facebook.put("clientId", clientId);
        }
      }

      if (facebookRaw.containsKey("redirectUris")) {
        List<String> allowlist = asStringList(merged.get("redirectUris"));
        List<String> redirectUris = normalizeRedirectUris(facebookRaw.get("redirectUris"));
        for (String uri : redirectUris) {
          if (!allowlist.contains(uri)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "oauth_facebook_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          facebook.remove("redirectUris");
        } else {
          facebook.put("redirectUris", redirectUris);
        }
      }

      // Never store plaintext secrets in oauth_config.
      facebook.remove("clientSecret");
      facebook.remove("clientSecretSet");

      if (facebookRaw.containsKey("clientSecret")) {
        String secret = asTrimmedStringAllowEmpty(facebookRaw.get("clientSecret"));
        if (secret == null || secret.isEmpty()) {
          settings.setOauthFacebookClientSecretEnc(null);
        } else {
          settings.setOauthFacebookClientSecretEnc(oauthClientSecretEncryptor.encrypt(secret));
        }
      }

      boolean enabled = Boolean.TRUE.equals(facebook.get("enabled"));
      String clientId = facebook.get("clientId") instanceof String s ? s : null;
      boolean secretSet =
          settings.getOauthFacebookClientSecretEnc() != null
              && !settings.getOauthFacebookClientSecretEnc().trim().isEmpty();
      List<String> redirectUris = asStringList(facebook.get("redirectUris"));

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_facebook_client_id_required");
      }
      if (enabled && !secretSet) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_facebook_client_secret_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_facebook_redirect_uris_required");
      }

      if (facebook.isEmpty()) {
        merged.remove("facebook");
      } else {
        merged.put("facebook", facebook);
      }
    }

    // apple: enabled/teamId/keyId/serviceId/redirectUris/privateKeyRef in config, privateKey encrypted in column
    if (patch.containsKey("apple")) {
      Object appleObj = patch.get("apple");
      if (!(appleObj instanceof Map<?, ?> appleRaw)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_config_invalid");
      }

      Map<String, Object> existingApple = asObjectMap(merged.get("apple"));
      Map<String, Object> apple = new HashMap<>(existingApple);

      if (appleRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(appleRaw.get("enabled"));
        if (enabled == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_enabled_invalid");
        }
        apple.put("enabled", enabled);
      }

      if (appleRaw.containsKey("teamId")) {
        String teamId = asTrimmedString(appleRaw.get("teamId"));
        if (teamId == null) {
          apple.remove("teamId");
        } else {
          apple.put("teamId", teamId);
        }
      }

      if (appleRaw.containsKey("keyId")) {
        String keyId = asTrimmedString(appleRaw.get("keyId"));
        if (keyId == null) {
          apple.remove("keyId");
        } else {
          apple.put("keyId", keyId);
        }
      }

      if (appleRaw.containsKey("serviceId")) {
        String serviceId = asTrimmedString(appleRaw.get("serviceId"));
        if (serviceId == null) {
          apple.remove("serviceId");
        } else {
          apple.put("serviceId", serviceId);
        }
      }

      if (appleRaw.containsKey("privateKeyRef")) {
        String ref = asTrimmedString(appleRaw.get("privateKeyRef"));
        if (ref == null) {
          apple.remove("privateKeyRef");
        } else {
          apple.put("privateKeyRef", ref);
        }
      }

      if (appleRaw.containsKey("redirectUris")) {
        List<String> allowlist = asStringList(merged.get("redirectUris"));
        List<String> redirectUris = normalizeRedirectUris(appleRaw.get("redirectUris"));
        for (String uri : redirectUris) {
          if (!allowlist.contains(uri)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "oauth_apple_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          apple.remove("redirectUris");
        } else {
          apple.put("redirectUris", redirectUris);
        }
      }

      // Never store plaintext secrets in oauth_config.
      apple.remove("privateKey");
      apple.remove("privateKeySet");

      if (appleRaw.containsKey("privateKey")) {
        String privateKey = asTrimmedStringAllowEmpty(appleRaw.get("privateKey"));
        if (privateKey == null || privateKey.isEmpty()) {
          settings.setOauthApplePrivateKeyEnc(null);
        } else {
          settings.setOauthApplePrivateKeyEnc(oauthClientSecretEncryptor.encrypt(privateKey));
        }
      }

      boolean enabled = Boolean.TRUE.equals(apple.get("enabled"));
      String teamId = apple.get("teamId") instanceof String s ? s : null;
      String keyId = apple.get("keyId") instanceof String s ? s : null;
      String serviceId = apple.get("serviceId") instanceof String s ? s : null;
      String privateKeyRef = apple.get("privateKeyRef") instanceof String s ? s : null;
      boolean privateKeySet =
          settings.getOauthApplePrivateKeyEnc() != null
              && !settings.getOauthApplePrivateKeyEnc().trim().isEmpty();
      List<String> redirectUris = asStringList(apple.get("redirectUris"));

      if (enabled && (teamId == null || teamId.trim().isEmpty())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_team_id_required");
      }
      if (enabled && (keyId == null || keyId.trim().isEmpty())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_key_id_required");
      }
      if (enabled && (serviceId == null || serviceId.trim().isEmpty())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_service_id_required");
      }
      if (enabled && !(privateKeySet || (privateKeyRef != null && !privateKeyRef.trim().isEmpty()))) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_apple_private_key_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "oauth_apple_redirect_uris_required");
      }

      if (apple.isEmpty()) {
        merged.remove("apple");
      } else {
        merged.put("apple", apple);
      }
    }

    // Pass through other config keys as-is.
    for (Map.Entry<String, Object> entry : patch.entrySet()) {
      String key = entry.getKey();
      if (key == null) {
        continue;
      }
      if (key.equals("google")
          || key.equals("github")
          || key.equals("microsoft")
          || key.equals("facebook")
          || key.equals("apple")
          || key.equals("redirectUris")) {
        continue;
      }
      merged.put(key, entry.getValue());
    }

    settings.setOauthConfig(merged);
    return projectSettingsRepository.save(settings);
  }

  private void enforceOrgAccess(UUID orgId, OrgMemberPrincipal principal) {
    if (principal == null) {
      throw new IllegalStateException("Authenticated org member principal is required.");
    }
    if (!orgId.equals(principal.orgId())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "org_access_denied");
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
        .map(ProjectOAuthSettingsApplicationService::asTrimmedString)
        .filter(s -> s != null)
        .map(ProjectOAuthSettingsApplicationService::validateRedirectUri)
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
}
