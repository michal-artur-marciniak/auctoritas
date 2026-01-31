package dev.auctoritas.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.oauth.OAuthConnection;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.organization.OrganizationStatus;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class OAuthConnectionRepositoryTest {

  @Autowired private OAuthConnectionRepository oauthConnectionRepository;
  @Autowired private EntityManager entityManager;

  private Project testProject;
  private EndUser testUser;

  @BeforeEach
  void setUp() {
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-oauth-conn");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);
    entityManager.flush();

    ProjectSettings settings = new ProjectSettings();

    testProject = new Project();
    testProject.setOrganization(org);
    testProject.setName("Test Project");
    testProject.setSlug("test-project-oauth-conn");
    testProject.setStatus(ProjectStatus.ACTIVE);
    testProject.setSettings(settings);
    settings.setProject(testProject);

    entityManager.persist(testProject);
    entityManager.flush();

    testUser = new EndUser();
    testUser.setProject(testProject);
    testUser.setEmail("user@example.com");
    testUser.setPasswordHash("hash");
    testUser.setName("Test User");
    testUser.setEmailVerified(false);
    entityManager.persist(testUser);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find oauth connection by project/provider/provider user id")
  void shouldFindByProjectProviderAndProviderUserId() {
    OAuthConnection conn = new OAuthConnection();
    conn.setProject(testProject);
    conn.setUser(testUser);
    conn.setProvider("google");
    conn.setProviderUserId("google-user-123");
    conn.setEmail("user@example.com");
    entityManager.persist(conn);
    entityManager.flush();

    Optional<OAuthConnection> found =
        oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
            testProject.getId(), "google", "google-user-123");
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("user@example.com");
  }

  @Test
  @DisplayName("Should enforce unique provider+providerUserId per project")
  void shouldEnforceUniqueProviderAndProviderUserIdPerProject() {
    OAuthConnection first = new OAuthConnection();
    first.setProject(testProject);
    first.setUser(testUser);
    first.setProvider("google");
    first.setProviderUserId("google-user-123");
    first.setEmail("user@example.com");
    entityManager.persist(first);
    entityManager.flush();

    EndUser otherUser = new EndUser();
    otherUser.setProject(testProject);
    otherUser.setEmail("other@example.com");
    otherUser.setPasswordHash("hash2");
    otherUser.setName("Other User");
    otherUser.setEmailVerified(false);
    entityManager.persist(otherUser);
    entityManager.flush();

    OAuthConnection second = new OAuthConnection();
    second.setProject(testProject);
    second.setUser(otherUser);
    second.setProvider("google");
    second.setProviderUserId("google-user-123");
    second.setEmail("other@example.com");
    entityManager.persist(second);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(jakarta.persistence.PersistenceException.class);
  }
}
