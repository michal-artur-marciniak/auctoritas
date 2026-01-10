package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.common.enums.ApiKeyStatus;
import dev.auctoritas.common.enums.OrganizationStatus;
import dev.auctoritas.common.enums.ProjectStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class ApiKeyRepositoryTest {

  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private ProjectSettingsRepository settingsRepository;
  @Autowired private EntityManager entityManager;

  private Organization testOrg;
  private Project testProject;
  private ApiKey testApiKey;

  @BeforeEach
  void setUp() {
    testOrg = new Organization();
    testOrg.setName("Test Org");
    testOrg.setSlug("test-org-apikey");
    testOrg.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(testOrg);
    entityManager.flush();

    ProjectSettings settings = new ProjectSettings();
    entityManager.persist(settings);
    entityManager.flush();

    testProject = new Project();
    testProject.setOrganization(testOrg);
    testProject.setName("Test Project");
    testProject.setSlug("test-project-apikey");
    testProject.setSettings(settings);
    entityManager.persist(testProject);
    entityManager.flush();

    testApiKey = new ApiKey();
    testApiKey.setProject(testProject);
    testApiKey.setName("Production Key");
    testApiKey.setPrefix("pk_live_");
    testApiKey.setKeyHash("abc123def456hash789");
    testApiKey.setStatus(ApiKeyStatus.ACTIVE);
    testApiKey.setLastUsedAt(LocalDateTime.now());
    entityManager.persist(testApiKey);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find API key by hash and status")
  void shouldFindByHashAndStatus() {
    Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndStatus("abc123def456hash789", ApiKeyStatus.ACTIVE);
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Production Key");
    assertThat(found.get().getPrefix()).isEqualTo("pk_live_");
  }

  @Test
  @DisplayName("Should return empty when key not found")
  void shouldReturnEmptyWhenKeyNotFound() {
    Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndStatus("nonexistent", ApiKeyStatus.ACTIVE);
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find API key by project ID and name")
  void shouldFindByProjectIdAndName() {
    Optional<ApiKey> found = apiKeyRepository.findByProjectIdAndName(testProject.getId(), "Production Key");
    assertThat(found).isPresent();
    assertThat(found.get().getKeyHash()).isEqualTo("abc123def456hash789");
  }

  @Test
  @DisplayName("Should check if API key name exists in project")
  void shouldCheckNameExists() {
    assertThat(apiKeyRepository.existsByProjectIdAndName(testProject.getId(), "Production Key")).isTrue();
    assertThat(apiKeyRepository.existsByProjectIdAndName(testProject.getId(), "Other Key")).isFalse();
  }

  @Test
  @DisplayName("Should not find key with different status")
  void shouldNotFindWithDifferentStatus() {
    ApiKey revokedKey = new ApiKey();
    revokedKey.setProject(testProject);
    revokedKey.setName("Revoked Key");
    revokedKey.setPrefix("pk_test_");
    revokedKey.setKeyHash("revokedhash123");
    revokedKey.setStatus(ApiKeyStatus.REVOKED);
    revokedKey.setLastUsedAt(LocalDateTime.now());
    entityManager.persist(revokedKey);
    entityManager.flush();

    Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndStatus("revokedhash123", ApiKeyStatus.ACTIVE);
    assertThat(found).isEmpty();
  }
}
