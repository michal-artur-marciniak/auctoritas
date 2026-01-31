package dev.auctoritas.auth.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.infrastructure.config.JpaConfig;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.oauth.OAuthConnection;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.project.Slug;
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
    Organization org = Organization.create("Test Org", Slug.of("test-org-oauth-conn"));
    entityManager.persist(org);
    entityManager.flush();

    testProject = Project.create(org, "Test Project", Slug.of("test-project-oauth-conn"));
    entityManager.persist(testProject);
    entityManager.flush();

    testUser = EndUser.create(testProject, Email.of("user@example.com"), Password.fromHash("hash"), "Test User");
    entityManager.persist(testUser);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find oauth connection by project/provider/provider user id")
  void shouldFindByProjectProviderAndProviderUserId() {
    OAuthConnection conn = OAuthConnection.establish(
        testProject,
        testUser,
        "google",
        "google-user-123",
        "user@example.com");
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
    OAuthConnection first = OAuthConnection.establish(
        testProject,
        testUser,
        "google",
        "google-user-123",
        "user@example.com");
    entityManager.persist(first);
    entityManager.flush();

    EndUser otherUser = EndUser.create(testProject, Email.of("other@example.com"), Password.fromHash("hash2"), "Other User");
    entityManager.persist(otherUser);
    entityManager.flush();

    OAuthConnection second = OAuthConnection.establish(
        testProject,
        otherUser,
        "google",
        "google-user-123",
        "other@example.com");
    entityManager.persist(second);

    assertThatThrownBy(entityManager::flush)
        .isInstanceOf(jakarta.persistence.PersistenceException.class);
  }
}
