package dev.auctoritas.auth.domain.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.Slug;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProjectOAuthSettingsValidatorTest {
  private final ProjectOAuthSettingsValidator validator = new ProjectOAuthSettingsValidator();

  private ProjectSettings createSettings() {
    Organization org = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(org, "Test", Slug.of("test"));
    return project.getSettings();
  }

  @Test
  void validateRejectsInvalidRedirectUri() {
    ProjectSettings settings = createSettings();
    Map<String, Object> patch = new HashMap<>();
    patch.put("redirectUris", List.of("ftp://app.example.com/callback"));

    assertThatThrownBy(() -> validator.validate(settings, patch))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("oauth_redirect_uri_invalid");
  }

  @Test
  void validateNormalizesRedirectUris() {
    ProjectSettings settings = createSettings();
    Map<String, Object> patch = new HashMap<>();
    patch.put(
        "redirectUris",
        List.of(" https://app.example.com/callback ", "https://app.example.com/callback"));

    ProjectOAuthSettingsUpdate update = validator.validate(settings, patch);

    @SuppressWarnings("unchecked")
    List<String> redirectUris = (List<String>) update.oauthConfig().get("redirectUris");
    assertThat(redirectUris).containsExactly("https://app.example.com/callback");
  }

  @Test
  void validateRejectsGithubRedirectUriNotInAllowlist() {
    ProjectSettings settings = createSettings();

    Map<String, Object> github = new HashMap<>();
    github.put("enabled", true);
    github.put("clientId", "gh-client-id");
    github.put("clientSecret", "gh-client-secret");
    github.put("redirectUris", List.of("https://not-allowed.example.com/callback"));

    Map<String, Object> patch = new HashMap<>();
    patch.put("redirectUris", List.of("https://allowed.example.com/callback"));
    patch.put("github", github);

    assertThatThrownBy(() -> validator.validate(settings, patch))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("oauth_github_redirect_uri_not_allowed");
  }

  @Test
  void validateRequiresGoogleClientIdWhenEnabled() {
    ProjectSettings settings = createSettings();

    Map<String, Object> google = new HashMap<>();
    google.put("enabled", true);
    google.put("clientSecret", "google-secret");

    Map<String, Object> patch = new HashMap<>();
    patch.put("google", google);

    assertThatThrownBy(() -> validator.validate(settings, patch))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("oauth_google_client_id_required");
  }

  @Test
  void validateRequiresGoogleSecretWhenEnabled() {
    ProjectSettings settings = createSettings();

    Map<String, Object> google = new HashMap<>();
    google.put("enabled", true);
    google.put("clientId", "google-client-id");

    Map<String, Object> patch = new HashMap<>();
    patch.put("google", google);

    assertThatThrownBy(() -> validator.validate(settings, patch))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("oauth_google_client_secret_required");
  }

  @Test
  void validateAcceptsApplePrivateKeyRef() {
    ProjectSettings settings = createSettings();

    Map<String, Object> apple = new HashMap<>();
    apple.put("enabled", true);
    apple.put("teamId", "TEAM123");
    apple.put("keyId", "KEY123");
    apple.put("serviceId", "com.example.app");
    apple.put("privateKeyRef", "private-key-ref");
    apple.put("redirectUris", List.of("https://app.example.com/apple/callback"));

    Map<String, Object> patch = new HashMap<>();
    patch.put("redirectUris", List.of("https://app.example.com/apple/callback"));
    patch.put("apple", apple);

    ProjectOAuthSettingsUpdate update = validator.validate(settings, patch);

    @SuppressWarnings("unchecked")
    Map<String, Object> storedApple = (Map<String, Object>) update.oauthConfig().get("apple");
    assertThat(storedApple.get("privateKeyRef")).isEqualTo("private-key-ref");
    assertThat(storedApple).doesNotContainKey("privateKey");
    assertThat(update.applePrivateKey().provided()).isFalse();
  }
}
