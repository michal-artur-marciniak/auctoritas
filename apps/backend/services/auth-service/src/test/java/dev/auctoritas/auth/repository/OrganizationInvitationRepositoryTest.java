package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.organization.OrganizationInvitation;
import dev.auctoritas.auth.domain.organization.OrgMemberRole;
import dev.auctoritas.auth.domain.organization.OrganizationStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class OrganizationInvitationRepositoryTest {

  @Autowired private OrganizationInvitationRepository invitationRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private EntityManager entityManager;

  private Organization testOrg;
  private OrganizationInvitation testInvitation;

  @BeforeEach
  void setUp() {
    testOrg = new Organization();
    testOrg.setName("Test Org");
    testOrg.setSlug("test-org-invite");
    testOrg.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(testOrg);
    entityManager.flush();

    testInvitation = new OrganizationInvitation();
    testInvitation.setEmail("invitee@test.com");
    testInvitation.setRole(OrgMemberRole.MEMBER);
    testInvitation.setToken("test-invite-token-123");
    testInvitation.setOrganization(testOrg);
    testInvitation.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
    entityManager.persist(testInvitation);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find invitation by token")
  void shouldFindByToken() {
    Optional<OrganizationInvitation> found = invitationRepository.findByToken("test-invite-token-123");
    assertThat(found).isPresent();
    assertThat(found.get().getEmail()).isEqualTo("invitee@test.com");
  }

  @Test
  @DisplayName("Should return empty when token not found")
  void shouldReturnEmptyWhenTokenNotFound() {
    Optional<OrganizationInvitation> found = invitationRepository.findByToken("nonexistent-token");
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should find invitation by email and organization ID")
  void shouldFindByEmailAndOrganizationId() {
    Optional<OrganizationInvitation> found = invitationRepository.findByEmailAndOrganizationId(
        "invitee@test.com", testOrg.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getRole()).isEqualTo(OrgMemberRole.MEMBER);
  }

  @Test
  @DisplayName("Should find expired invitations")
  void shouldFindExpiredInvitations() {
    OrganizationInvitation expired = new OrganizationInvitation();
    expired.setEmail("expired@test.com");
    expired.setRole(OrgMemberRole.MEMBER);
    expired.setToken("expired-token-123");
    expired.setOrganization(testOrg);
    expired.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
    entityManager.persist(expired);
    entityManager.flush();

    List<OrganizationInvitation> expiredInvitations = invitationRepository.findByExpiresAtBefore(Instant.now());
    assertThat(expiredInvitations).isNotEmpty();
  }
}
