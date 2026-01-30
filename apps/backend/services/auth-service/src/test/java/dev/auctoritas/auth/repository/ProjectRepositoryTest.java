package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.domain.project.ProjectStatus;
import dev.auctoritas.auth.shared.enums.OrganizationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ProjectRepositoryTest {

  @Autowired private ProjectRepository projectRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private EntityManager entityManager;

  private Organization testOrg;
  private Project testProject;

  @BeforeEach
  void setUp() {
    testOrg = new Organization();
    testOrg.setName("Test Org");
    testOrg.setSlug("test-org-project");
    testOrg.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(testOrg);
    entityManager.flush();

    ProjectSettings settings = new ProjectSettings();
    settings.setMinLength(8);
    settings.setRequireUppercase(true);
    settings.setRequireNumbers(true);

    testProject = new Project();
    testProject.setOrganization(testOrg);
    testProject.setName("Test Project");
    testProject.setSlug("test-project");
    testProject.setStatus(ProjectStatus.ACTIVE);
    testProject.setSettings(settings);
    settings.setProject(testProject);

    entityManager.persist(testProject);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find project by slug and organization ID")
  void shouldFindBySlugAndOrganizationId() {
    Optional<Project> found = projectRepository.findBySlugAndOrganizationId("test-project", testOrg.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Project");
  }

  @Test
  @DisplayName("Should return empty when slug not found")
  void shouldReturnEmptyWhenSlugNotFound() {
    Optional<Project> found = projectRepository.findBySlugAndOrganizationId("nonexistent", testOrg.getId());
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should check if slug exists in organization")
  void shouldCheckSlugExists() {
    assertThat(projectRepository.existsBySlugAndOrganizationId("test-project", testOrg.getId())).isTrue();
    assertThat(projectRepository.existsBySlugAndOrganizationId("other", testOrg.getId())).isFalse();
  }

  @Test
  @DisplayName("Should find all projects by organization ID")
  void shouldFindAllByOrganizationId() {
    List<Project> projects = projectRepository.findAllByOrganizationId(testOrg.getId());
    assertThat(projects).hasSize(1);
    assertThat(projects.get(0).getName()).isEqualTo("Test Project");
  }

  @Test
  @DisplayName("Should find projects by status")
  void shouldFindByStatus() {
    List<Project> activeProjects = projectRepository.findByOrganizationIdAndStatus(testOrg.getId(), ProjectStatus.ACTIVE);
    assertThat(activeProjects).isNotEmpty();
  }

  @Test
  @DisplayName("Should enforce unique slug per organization")
  void shouldEnforceUniqueSlugPerOrganization() {
    Project duplicate = new Project();
    duplicate.setOrganization(testOrg);
    duplicate.setName("Duplicate");
    duplicate.setSlug("test-project");
    entityManager.persist(duplicate);
    assertThatThrownBy(() -> entityManager.flush())
        .isInstanceOf(jakarta.persistence.PersistenceException.class);
  }
}
