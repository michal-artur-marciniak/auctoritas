package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.ProjectOAuthSettingsRequest;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.security.OrgMemberPrincipal;
import dev.auctoritas.common.enums.OrgMemberRole;
import dev.auctoritas.common.enums.OrganizationStatus;
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
  private OrgMemberPrincipal principal;

  @BeforeEach
  void setUp() {
    org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-project-oauth-settings");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();
    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-oauth-settings");
    project.setSettings(settings);
    settings.setProject(project);
    entityManager.persist(project);

    entityManager.flush();
    entityManager.clear();

    principal =
        new OrgMemberPrincipal(UUID.randomUUID(), org.getId(), "admin@example.com", OrgMemberRole.ADMIN);
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
}
