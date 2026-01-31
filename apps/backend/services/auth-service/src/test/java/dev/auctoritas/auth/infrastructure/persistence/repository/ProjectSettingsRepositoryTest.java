package dev.auctoritas.auth.infrastructure.persistence.repository;

import dev.auctoritas.auth.infrastructure.config.JpaConfig;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.project.Slug;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ProjectSettingsRepositoryTest {

  @Autowired private ProjectSettingsRepository settingsRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private EntityManager entityManager;

  private Organization testOrg;
  private Project testProject;
  private ProjectSettings testSettings;

  @BeforeEach
  void setUp() {
    testOrg = Organization.create("Test Org", Slug.of("test-org-settings"));
    entityManager.persist(testOrg);
    entityManager.flush();

    // Project.create() automatically creates settings
    testProject = Project.create(testOrg, "Test Project", Slug.of("test-project-settings"));
    entityManager.persist(testProject);
    entityManager.flush();

    // Get the settings created by the factory and update them
    testSettings = testProject.getSettings();
    testSettings.updatePasswordPolicy(10, true, true, true, true, 5);
    testSettings.updateSessionSettings(1800, 604800, 10);
    testSettings.updateMfaSettings(true, false);
    entityManager.persist(testSettings);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should save and retrieve project settings")
  void shouldSaveAndRetrieveSettings() {
    Optional<ProjectSettings> found = settingsRepository.findById(testSettings.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getMinLength()).isEqualTo(10);
    assertThat(found.get().isRequireSpecialChars()).isTrue();
    assertThat(found.get().getMaxSessions()).isEqualTo(10);
  }

  @Test
  @DisplayName("Should have default values when not set")
  void shouldHaveDefaultValues() {
    // Project.create() creates settings with defaults
    Project defaultProject = Project.create(testOrg, "Default Project", Slug.of("default-project"));
    entityManager.persist(defaultProject);
    entityManager.flush();

    ProjectSettings defaultSettings = defaultProject.getSettings();
    Optional<ProjectSettings> found = settingsRepository.findById(defaultSettings.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getMinLength()).isEqualTo(8);
    assertThat(found.get().isRequireUppercase()).isTrue();
    assertThat(found.get().isRequireNumbers()).isTrue();
    assertThat(found.get().isRequireSpecialChars()).isFalse();
  }
}
