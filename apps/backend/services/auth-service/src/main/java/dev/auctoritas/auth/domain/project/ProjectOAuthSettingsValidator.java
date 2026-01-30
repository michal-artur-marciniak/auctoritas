package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.ProjectOAuthSettingsUpdate.SecretUpdate;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates and normalizes project OAuth settings updates.
 */
public class ProjectOAuthSettingsValidator {

  /**
   * Validates the requested OAuth settings patch and returns normalized updates.
   */
  public ProjectOAuthSettingsUpdate validate(ProjectSettings settings, Map<String, Object> requestConfig) {
    if (settings == null) {
      throw new IllegalArgumentException("settings is required");
    }

    Map<String, Object> merged =
        new HashMap<>(settings.getOauthConfig() == null ? Map.of() : settings.getOauthConfig());
    Map<String, Object> patch = requestConfig == null ? Map.of() : new HashMap<>(requestConfig);

    SecretUpdate googleSecret = SecretUpdate.none();
    SecretUpdate githubSecret = SecretUpdate.none();
    SecretUpdate microsoftSecret = SecretUpdate.none();
    SecretUpdate facebookSecret = SecretUpdate.none();
    SecretUpdate applePrivateKey = SecretUpdate.none();

    if (patch.containsKey("redirectUris")) {
      merged.put("redirectUris", normalizeRedirectUris(patch.get("redirectUris")));
    }

    if (patch.containsKey("google")) {
      Object googleObj = patch.get("google");
      if (!(googleObj instanceof Map<?, ?> googleRaw)) {
        throw invalid("oauth_google_config_invalid");
      }

      Map<String, Object> existingGoogle = asObjectMap(merged.get("google"));
      Map<String, Object> google = new HashMap<>(existingGoogle);

      if (googleRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(googleRaw.get("enabled"));
        if (enabled == null) {
          throw invalid("oauth_google_enabled_invalid");
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

      google.remove("clientSecret");
      google.remove("clientSecretSet");

      if (googleRaw.containsKey("clientSecret")) {
        googleSecret = toSecretUpdate(googleRaw.get("clientSecret"));
      }

      boolean enabled = Boolean.TRUE.equals(google.get("enabled"));
      String clientId = google.get("clientId") instanceof String s ? s : null;
      boolean secretSet = resolveSecretSet(googleSecret, settings.getOauthGoogleClientSecretEnc());

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw invalid("oauth_google_client_id_required");
      }
      if (enabled && !secretSet) {
        throw invalid("oauth_google_client_secret_required");
      }

      if (google.isEmpty()) {
        merged.remove("google");
      } else {
        merged.put("google", google);
      }
    }

    if (patch.containsKey("github")) {
      Object githubObj = patch.get("github");
      if (!(githubObj instanceof Map<?, ?> githubRaw)) {
        throw invalid("oauth_github_config_invalid");
      }

      Map<String, Object> existingGithub = asObjectMap(merged.get("github"));
      Map<String, Object> github = new HashMap<>(existingGithub);

      if (githubRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(githubRaw.get("enabled"));
        if (enabled == null) {
          throw invalid("oauth_github_enabled_invalid");
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
            throw invalid("oauth_github_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          github.remove("redirectUris");
        } else {
          github.put("redirectUris", redirectUris);
        }
      }

      github.remove("clientSecret");
      github.remove("clientSecretSet");

      if (githubRaw.containsKey("clientSecret")) {
        githubSecret = toSecretUpdate(githubRaw.get("clientSecret"));
      }

      boolean enabled = Boolean.TRUE.equals(github.get("enabled"));
      String clientId = github.get("clientId") instanceof String s ? s : null;
      boolean secretSet = resolveSecretSet(githubSecret, settings.getOauthGithubClientSecretEnc());
      List<String> redirectUris = asStringList(github.get("redirectUris"));

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw invalid("oauth_github_client_id_required");
      }
      if (enabled && !secretSet) {
        throw invalid("oauth_github_client_secret_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw invalid("oauth_github_redirect_uris_required");
      }

      if (github.isEmpty()) {
        merged.remove("github");
      } else {
        merged.put("github", github);
      }
    }

    if (patch.containsKey("microsoft")) {
      Object microsoftObj = patch.get("microsoft");
      if (!(microsoftObj instanceof Map<?, ?> microsoftRaw)) {
        throw invalid("oauth_microsoft_config_invalid");
      }

      Map<String, Object> existingMicrosoft = asObjectMap(merged.get("microsoft"));
      Map<String, Object> microsoft = new HashMap<>(existingMicrosoft);

      if (microsoftRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(microsoftRaw.get("enabled"));
        if (enabled == null) {
          throw invalid("oauth_microsoft_enabled_invalid");
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
            throw invalid("oauth_microsoft_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          microsoft.remove("redirectUris");
        } else {
          microsoft.put("redirectUris", redirectUris);
        }
      }

      microsoft.remove("clientSecret");
      microsoft.remove("clientSecretSet");

      if (microsoftRaw.containsKey("clientSecret")) {
        microsoftSecret = toSecretUpdate(microsoftRaw.get("clientSecret"));
      }

      boolean enabled = Boolean.TRUE.equals(microsoft.get("enabled"));
      String clientId = microsoft.get("clientId") instanceof String s ? s : null;
      boolean secretSet = resolveSecretSet(microsoftSecret, settings.getOauthMicrosoftClientSecretEnc());
      List<String> redirectUris = asStringList(microsoft.get("redirectUris"));

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw invalid("oauth_microsoft_client_id_required");
      }
      if (enabled && !secretSet) {
        throw invalid("oauth_microsoft_client_secret_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw invalid("oauth_microsoft_redirect_uris_required");
      }

      if (microsoft.isEmpty()) {
        merged.remove("microsoft");
      } else {
        merged.put("microsoft", microsoft);
      }
    }

    if (patch.containsKey("facebook")) {
      Object facebookObj = patch.get("facebook");
      if (!(facebookObj instanceof Map<?, ?> facebookRaw)) {
        throw invalid("oauth_facebook_config_invalid");
      }

      Map<String, Object> existingFacebook = asObjectMap(merged.get("facebook"));
      Map<String, Object> facebook = new HashMap<>(existingFacebook);

      if (facebookRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(facebookRaw.get("enabled"));
        if (enabled == null) {
          throw invalid("oauth_facebook_enabled_invalid");
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
            throw invalid("oauth_facebook_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          facebook.remove("redirectUris");
        } else {
          facebook.put("redirectUris", redirectUris);
        }
      }

      facebook.remove("clientSecret");
      facebook.remove("clientSecretSet");

      if (facebookRaw.containsKey("clientSecret")) {
        facebookSecret = toSecretUpdate(facebookRaw.get("clientSecret"));
      }

      boolean enabled = Boolean.TRUE.equals(facebook.get("enabled"));
      String clientId = facebook.get("clientId") instanceof String s ? s : null;
      boolean secretSet = resolveSecretSet(facebookSecret, settings.getOauthFacebookClientSecretEnc());
      List<String> redirectUris = asStringList(facebook.get("redirectUris"));

      if (enabled && (clientId == null || clientId.trim().isEmpty())) {
        throw invalid("oauth_facebook_client_id_required");
      }
      if (enabled && !secretSet) {
        throw invalid("oauth_facebook_client_secret_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw invalid("oauth_facebook_redirect_uris_required");
      }

      if (facebook.isEmpty()) {
        merged.remove("facebook");
      } else {
        merged.put("facebook", facebook);
      }
    }

    if (patch.containsKey("apple")) {
      Object appleObj = patch.get("apple");
      if (!(appleObj instanceof Map<?, ?> appleRaw)) {
        throw invalid("oauth_apple_config_invalid");
      }

      Map<String, Object> existingApple = asObjectMap(merged.get("apple"));
      Map<String, Object> apple = new HashMap<>(existingApple);

      if (appleRaw.containsKey("enabled")) {
        Boolean enabled = asBoolean(appleRaw.get("enabled"));
        if (enabled == null) {
          throw invalid("oauth_apple_enabled_invalid");
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
            throw invalid("oauth_apple_redirect_uri_not_allowed");
          }
        }
        if (redirectUris.isEmpty()) {
          apple.remove("redirectUris");
        } else {
          apple.put("redirectUris", redirectUris);
        }
      }

      apple.remove("privateKey");
      apple.remove("privateKeySet");

      if (appleRaw.containsKey("privateKey")) {
        applePrivateKey = toSecretUpdate(appleRaw.get("privateKey"));
      }

      boolean enabled = Boolean.TRUE.equals(apple.get("enabled"));
      String teamId = apple.get("teamId") instanceof String s ? s : null;
      String keyId = apple.get("keyId") instanceof String s ? s : null;
      String serviceId = apple.get("serviceId") instanceof String s ? s : null;
      String privateKeyRef = apple.get("privateKeyRef") instanceof String s ? s : null;
      boolean privateKeySet = resolveSecretSet(applePrivateKey, settings.getOauthApplePrivateKeyEnc());
      List<String> redirectUris = asStringList(apple.get("redirectUris"));

      if (enabled && (teamId == null || teamId.trim().isEmpty())) {
        throw invalid("oauth_apple_team_id_required");
      }
      if (enabled && (keyId == null || keyId.trim().isEmpty())) {
        throw invalid("oauth_apple_key_id_required");
      }
      if (enabled && (serviceId == null || serviceId.trim().isEmpty())) {
        throw invalid("oauth_apple_service_id_required");
      }
      if (enabled
          && !(privateKeySet || (privateKeyRef != null && !privateKeyRef.trim().isEmpty()))) {
        throw invalid("oauth_apple_private_key_required");
      }
      if (enabled && redirectUris.isEmpty()) {
        throw invalid("oauth_apple_redirect_uris_required");
      }

      if (apple.isEmpty()) {
        merged.remove("apple");
      } else {
        merged.put("apple", apple);
      }
    }

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
      throw invalid("oauth_config_key_invalid");
    }

    return new ProjectOAuthSettingsUpdate(
        merged, googleSecret, githubSecret, microsoftSecret, facebookSecret, applePrivateKey);
  }

  private static DomainValidationException invalid(String errorCode) {
    return new DomainValidationException(errorCode);
  }

  private static boolean resolveSecretSet(SecretUpdate update, String existingEncrypted) {
    if (update != null && update.provided()) {
      return update.value() != null && !update.value().trim().isEmpty();
    }
    return existingEncrypted != null && !existingEncrypted.trim().isEmpty();
  }

  private static Map<String, Object> asObjectMap(Object value) {
    Map<String, Object> result = new HashMap<>();
    if (!(value instanceof Map<?, ?> map)) {
      return result;
    }
    for (Map.Entry<?, ?> entry : map.entrySet()) {
      Object key = entry.getKey();
      if (key instanceof String keyString) {
        result.put(keyString, entry.getValue());
      }
    }
    return result;
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

  private static SecretUpdate toSecretUpdate(Object rawValue) {
    String secret = asTrimmedStringAllowEmpty(rawValue);
    if (secret == null || secret.isEmpty()) {
      return SecretUpdate.clear();
    }
    return SecretUpdate.set(secret);
  }

  private static List<String> normalizeRedirectUris(Object value) {
    if (value == null) {
      return List.of();
    }
    if (!(value instanceof List<?> list)) {
      throw invalid("oauth_redirect_uris_invalid");
    }
    return list.stream()
        .map(ProjectOAuthSettingsValidator::asTrimmedString)
        .filter(s -> s != null)
        .map(ProjectOAuthSettingsValidator::validateRedirectUri)
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
        throw invalid("oauth_redirect_uri_invalid");
      }
      if (uri.getHost() == null) {
        throw invalid("oauth_redirect_uri_invalid");
      }
      if (uri.getFragment() != null) {
        throw invalid("oauth_redirect_uri_invalid");
      }
      return uri.toString();
    } catch (URISyntaxException e) {
      throw new DomainValidationException("oauth_redirect_uri_invalid", e);
    }
  }
}
