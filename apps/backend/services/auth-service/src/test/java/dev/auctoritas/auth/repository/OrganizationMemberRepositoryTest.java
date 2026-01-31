package dev.auctoritas.auth.repository;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationInvitation;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import dev.auctoritas.auth.domain.organization.OrganizationMemberStatus;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.project.Slug;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class OrganizationMemberRepositoryTest {

  @Autowired private OrganizationMemberRepository memberRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private EntityManager entityManager;

  private Organization testOrg;
  private OrganizationMember testMember;

  @BeforeEach
  void setUp() {
    testOrg = Organization.create("Test Org", Slug.of("test-org-member"));
    entityManager.persist(testOrg);
    entityManager.flush();

    testMember = OrganizationMember.create(
        testOrg,
        Email.of("member@test.com"),
        "hashedpassword123",
        "Test Member",
        OrganizationMemberRole.MEMBER,
        true);
    testOrg.addMember(testMember);
    entityManager.persist(testMember);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should find member by email and organization ID")
  void shouldFindByEmailAndOrganizationId() {
    Optional<OrganizationMember> found = memberRepository.findByEmailAndOrganizationId(
        "member@test.com", testOrg.getId());
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Test Member");
  }

  @Test
  @DisplayName("Should return empty when email not found")
  void shouldReturnEmptyWhenEmailNotFound() {
    Optional<OrganizationMember> found = memberRepository.findByEmailAndOrganizationId(
        "nonexistent@test.com", testOrg.getId());
    assertThat(found).isEmpty();
  }

  @Test
  @DisplayName("Should check if email exists in organization")
  void shouldCheckEmailExists() {
    assertThat(memberRepository.existsByEmailAndOrganizationId(
        "member@test.com", testOrg.getId())).isTrue();
    assertThat(memberRepository.existsByEmailAndOrganizationId(
        "other@test.com", testOrg.getId())).isFalse();
  }

  @Test
  @DisplayName("Should find all members by organization ID")
  void shouldFindByOrganizationId() {
    List<OrganizationMember> members = memberRepository.findByOrganizationId(testOrg.getId());
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getEmail()).isEqualTo("member@test.com");
  }

  @Test
  @DisplayName("Should find members by status")
  void shouldFindByStatus() {
    List<OrganizationMember> activeMembers = memberRepository.findByStatus(OrganizationMemberStatus.ACTIVE);
    assertThat(activeMembers).isNotEmpty();
  }
}
