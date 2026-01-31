package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.project.Slug;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProjectServiceOAuthSettingsTest {

  @Autowired private EntityManager entityManager;
  @Autowired private ProjectService projectService;

  private Organization org;
  private Project project;
  private OrganizationMemberPrincipal principal;

  @BeforeEach
  void setUp() {
    org = Organization.create("Test Org", Slug.of("test-org-project-oauth-settings"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-oauth-settings"));
    entityManager.persist(project);

    entityManager.flush();
    entityManager.clear();

    principal =
        new OrganizationMemberPrincipal(UUID.randomUUID(), org.getId(), "admin@example.com", OrganizationMemberRole.ADMIN);
  }

  @Test
  void updateOAuthSettingsStoresGithubSecretEncryptedAndMasksInResponse() {
    Map<String, Object> github = new HashMap<>();
    github.put("enabled", true);
    github.put("clientId", "gh-client-id");
    github.put("clientSecret", "gh-client-secret");
    github.put("redirectUris", List.of("https://app.example.com/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/callback"));
    config.put("github", github);

    var response =
        projectService.updateOAuthSettings(
            org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config));

    ProjectSettings persisted =
        entityManager
            .createQuery(
                "select s from ProjectSettings s join fetch s.project p where p.id = :pid",
                ProjectSettings.class)
            .setParameter("pid", project.getId())
            .getSingleResult();

    assertThat(persisted.getOauthGithubClientSecretEnc()).isNotBlank();
    assertThat(persisted.getOauthGithubClientSecretEnc()).isNotEqualTo("gh-client-secret");

    @SuppressWarnings("unchecked")
    Map<String, Object> storedGithub =
        (Map<String, Object>) ((Map<String, Object>) persisted.getOauthConfig()).get("github");
    assertThat(storedGithub).doesNotContainKey("clientSecret");
    assertThat(storedGithub).doesNotContainKey("clientSecretSet");

    @SuppressWarnings("unchecked")
    Map<String, Object> safeGithub = (Map<String, Object>) response.oauthConfig().get("github");
    assertThat(safeGithub.get("clientSecretSet")).isEqualTo(true);
    assertThat(safeGithub.get("clientSecret")).isEqualTo("********");
  }

  @Test
  void updateOAuthSettingsRejectsGithubRedirectUriNotInAllowlist() {
    Map<String, Object> github = new HashMap<>();
    github.put("enabled", true);
    github.put("clientId", "gh-client-id");
    github.put("clientSecret", "gh-client-secret");
    github.put("redirectUris", List.of("https://not-allowed.example.com/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/callback"));
    config.put("github", github);

    assertThatThrownBy(
            () ->
                projectService.updateOAuthSettings(
                    org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config)))
        .hasMessageContaining("oauth_github_redirect_uri_not_allowed");
  }

  @Test
  void updateOAuthSettingsStoresMicrosoftSecretEncryptedAndMasksInResponse() {
    Map<String, Object> microsoft = new HashMap<>();
    microsoft.put("enabled", true);
    microsoft.put("clientId", "ms-client-id");
    microsoft.put("tenant", "common");
    microsoft.put("clientSecret", "ms-client-secret");
    microsoft.put("redirectUris", List.of("https://app.example.com/ms/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/ms/callback"));
    config.put("microsoft", microsoft);

    var response =
        projectService.updateOAuthSettings(
            org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config));

    ProjectSettings persisted =
        entityManager
            .createQuery(
                "select s from ProjectSettings s join fetch s.project p where p.id = :pid",
                ProjectSettings.class)
            .setParameter("pid", project.getId())
            .getSingleResult();

    assertThat(persisted.getOauthMicrosoftClientSecretEnc()).isNotBlank();
    assertThat(persisted.getOauthMicrosoftClientSecretEnc()).isNotEqualTo("ms-client-secret");

    @SuppressWarnings("unchecked")
    Map<String, Object> storedMicrosoft =
        (Map<String, Object>) ((Map<String, Object>) persisted.getOauthConfig()).get("microsoft");
    assertThat(storedMicrosoft).doesNotContainKey("clientSecret");
    assertThat(storedMicrosoft).doesNotContainKey("clientSecretSet");

    @SuppressWarnings("unchecked")
    Map<String, Object> safeMicrosoft =
        (Map<String, Object>) response.oauthConfig().get("microsoft");
    assertThat(safeMicrosoft.get("clientSecretSet")).isEqualTo(true);
    assertThat(safeMicrosoft.get("clientSecret")).isEqualTo("********");
    assertThat(safeMicrosoft.get("tenant")).isEqualTo("common");
  }

  @Test
  void updateOAuthSettingsRejectsMicrosoftRedirectUriNotInAllowlist() {
    Map<String, Object> microsoft = new HashMap<>();
    microsoft.put("enabled", true);
    microsoft.put("clientId", "ms-client-id");
    microsoft.put("clientSecret", "ms-client-secret");
    microsoft.put("redirectUris", List.of("https://not-allowed.example.com/ms/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/ms/callback"));
    config.put("microsoft", microsoft);

    assertThatThrownBy(
            () ->
                projectService.updateOAuthSettings(
                    org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config)))
        .hasMessageContaining("oauth_microsoft_redirect_uri_not_allowed");
  }

  @Test
  void updateOAuthSettingsStoresFacebookSecretEncryptedAndMasksInResponse() {
    Map<String, Object> facebook = new HashMap<>();
    facebook.put("enabled", true);
    facebook.put("clientId", "fb-client-id");
    facebook.put("clientSecret", "fb-client-secret");
    facebook.put("redirectUris", List.of("https://app.example.com/fb/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/fb/callback"));
    config.put("facebook", facebook);

    var response =
        projectService.updateOAuthSettings(
            org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config));

    ProjectSettings persisted =
        entityManager
            .createQuery(
                "select s from ProjectSettings s join fetch s.project p where p.id = :pid",
                ProjectSettings.class)
            .setParameter("pid", project.getId())
            .getSingleResult();

    assertThat(persisted.getOauthFacebookClientSecretEnc()).isNotBlank();
    assertThat(persisted.getOauthFacebookClientSecretEnc()).isNotEqualTo("fb-client-secret");

    @SuppressWarnings("unchecked")
    Map<String, Object> storedFacebook =
        (Map<String, Object>) ((Map<String, Object>) persisted.getOauthConfig()).get("facebook");
    assertThat(storedFacebook).doesNotContainKey("clientSecret");
    assertThat(storedFacebook).doesNotContainKey("clientSecretSet");

    @SuppressWarnings("unchecked")
    Map<String, Object> safeFacebook = (Map<String, Object>) response.oauthConfig().get("facebook");
    assertThat(safeFacebook.get("clientSecretSet")).isEqualTo(true);
    assertThat(safeFacebook.get("clientSecret")).isEqualTo("********");
  }

  @Test
  void updateOAuthSettingsRejectsFacebookRedirectUriNotInAllowlist() {
    Map<String, Object> facebook = new HashMap<>();
    facebook.put("enabled", true);
    facebook.put("clientId", "fb-client-id");
    facebook.put("clientSecret", "fb-client-secret");
    facebook.put("redirectUris", List.of("https://not-allowed.example.com/fb/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/fb/callback"));
    config.put("facebook", facebook);

    assertThatThrownBy(
            () ->
                projectService.updateOAuthSettings(
                    org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config)))
        .hasMessageContaining("oauth_facebook_redirect_uri_not_allowed");
  }

  @Test
  void updateOAuthSettingsStoresApplePrivateKeyEncryptedAndMasksInResponse() {
    Map<String, Object> apple = new HashMap<>();
    apple.put("enabled", true);
    apple.put("teamId", "TEAM123");
    apple.put("keyId", "KEY123");
    apple.put("serviceId", "com.example.app");
    apple.put("privateKey", "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----");
    apple.put("redirectUris", List.of("https://app.example.com/apple/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/apple/callback"));
    config.put("apple", apple);

    var response =
        projectService.updateOAuthSettings(
            org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config));

    ProjectSettings persisted =
        entityManager
            .createQuery(
                "select s from ProjectSettings s join fetch s.project p where p.id = :pid",
                ProjectSettings.class)
            .setParameter("pid", project.getId())
            .getSingleResult();

    assertThat(persisted.getOauthApplePrivateKeyEnc()).isNotBlank();
    assertThat(persisted.getOauthApplePrivateKeyEnc())
        .isNotEqualTo("-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----");

    @SuppressWarnings("unchecked")
    Map<String, Object> storedApple =
        (Map<String, Object>) ((Map<String, Object>) persisted.getOauthConfig()).get("apple");
    assertThat(storedApple).doesNotContainKey("privateKey");
    assertThat(storedApple).doesNotContainKey("privateKeySet");

    @SuppressWarnings("unchecked")
    Map<String, Object> safeApple = (Map<String, Object>) response.oauthConfig().get("apple");
    assertThat(safeApple.get("privateKeySet")).isEqualTo(true);
    assertThat(safeApple.get("privateKey")).isEqualTo("********");
    assertThat(safeApple.get("teamId")).isEqualTo("TEAM123");
    assertThat(safeApple.get("keyId")).isEqualTo("KEY123");
    assertThat(safeApple.get("serviceId")).isEqualTo("com.example.app");
  }

  @Test
  void updateOAuthSettingsRejectsAppleRedirectUriNotInAllowlist() {
    Map<String, Object> apple = new HashMap<>();
    apple.put("enabled", true);
    apple.put("teamId", "TEAM123");
    apple.put("keyId", "KEY123");
    apple.put("serviceId", "com.example.app");
    apple.put("privateKey", "-----BEGIN PRIVATE KEY-----\nabc\n-----END PRIVATE KEY-----");
    apple.put("redirectUris", List.of("https://not-allowed.example.com/apple/callback"));

    Map<String, Object> config = new HashMap<>();
    config.put("redirectUris", List.of("https://app.example.com/apple/callback"));
    config.put("apple", apple);

    assertThatThrownBy(
            () ->
                projectService.updateOAuthSettings(
                    org.getId(), project.getId(), principal, new ProjectOAuthSettingsRequest(config)))
        .hasMessageContaining("oauth_apple_redirect_uri_not_allowed");
  }
}
