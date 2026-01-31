package dev.auctoritas.auth.infrastructure.persistence.repository;

import dev.auctoritas.auth.infrastructure.config.JpaConfig;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.project.ApiKeyStatus;
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
class ApiKeyRepositoryTest {

  @Autowired private ApiKeyRepository apiKeyRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private EntityManager entityManager;

  private Organization testOrg;
  private Project testProject;
  private ApiKey testApiKey;

  @BeforeEach
  void setUp() {
    testOrg = Organization.create("Test Org", Slug.of("test-org-apikey"));
    entityManager.persist(testOrg);
    entityManager.flush();

    testProject = Project.create(testOrg, "Test Project", Slug.of("test-project-apikey"));
    entityManager.persist(testProject);
    entityManager.flush();

    testApiKey = ApiKey.create(testProject, "Production Key", "pk_live_", "abc123def456hash789");
    testApiKey.recordUsage();
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
    ApiKey revokedKey = ApiKey.create(testProject, "Revoked Key", "pk_test_", "revokedhash123");
    revokedKey.revoke("test");
    entityManager.persist(revokedKey);
    entityManager.flush();

    Optional<ApiKey> found = apiKeyRepository.findByKeyHashAndStatus("revokedhash123", ApiKeyStatus.ACTIVE);
    assertThat(found).isEmpty();
  }
}
