package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.domain.organization.OrganizationStatus;
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
    testOrg = new Organization();
    testOrg.setName("Test Org");
    testOrg.setSlug("test-org-settings");
    testOrg.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(testOrg);
    entityManager.flush();

    testSettings = new ProjectSettings();
    testSettings.setMinLength(10);
    testSettings.setRequireUppercase(true);
    testSettings.setRequireNumbers(true);
    testSettings.setRequireSpecialChars(true);
    testSettings.setPasswordHistoryCount(5);
    testSettings.setAccessTokenTtlSeconds(1800);
    testSettings.setRefreshTokenTtlSeconds(604800);
    testSettings.setMaxSessions(10);
    testSettings.setMfaEnabled(true);
    testSettings.setMfaRequired(false);

    testProject = new Project();
    testProject.setOrganization(testOrg);
    testProject.setName("Test Project");
    testProject.setSlug("test-project-settings");
    testProject.setSettings(testSettings);
    testSettings.setProject(testProject);

    entityManager.persist(testProject);
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
    ProjectSettings defaultSettings = new ProjectSettings();
    Project defaultProject = new Project();
    defaultProject.setOrganization(testOrg);
    defaultProject.setName("Default Project");
    defaultProject.setSlug("default-project");
    defaultProject.setSettings(defaultSettings);
    defaultSettings.setProject(defaultProject);

    entityManager.persist(defaultProject);
    entityManager.flush();

    Optional<ProjectSettings> found = settingsRepository.findById(defaultSettings.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getMinLength()).isEqualTo(8);
    assertThat(found.get().isRequireUppercase()).isTrue();
    assertThat(found.get().isRequireNumbers()).isTrue();
    assertThat(found.get().isRequireSpecialChars()).isFalse();
  }
}
