package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.organization.Organization;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class OrganizationRepositoryTest {

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private EntityManager entityManager;

  private Organization testOrg;

  @BeforeEach
  void setUp() {
    testOrg = new Organization();
    testOrg.setName("Test Organization");
    testOrg.setSlug("test-org");
    testOrg.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(testOrg);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find organization by slug")
  void shouldFindBySlug() {
    Optional<Organization> found = organizationRepository.findBySlug("test-org");
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Organization");
  }

  @Test
  @DisplayName("Should return empty when slug not found")
  void shouldReturnEmptyWhenSlugNotFound() {
    Optional<Organization> found = organizationRepository.findBySlug("nonexistent");
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should enforce unique slug constraint")
  void shouldEnforceUniqueSlug() {
    Organization duplicate = new Organization();
    duplicate.setName("Duplicate");
    duplicate.setSlug("test-org");
    entityManager.persist(duplicate);
    assertThatThrownBy(() -> entityManager.flush())
        .isInstanceOf(jakarta.persistence.PersistenceException.class);
  }

  @Test
  @DisplayName("Should find organization by ID")
  void shouldFindById() {
    Optional<Organization> found = organizationRepository.findById(testOrg.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Organization");
  }
}
