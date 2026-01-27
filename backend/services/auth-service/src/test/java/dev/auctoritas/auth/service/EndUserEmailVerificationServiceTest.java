package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.api.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserEmailVerificationTokenRepository;
import dev.auctoritas.common.enums.ApiKeyStatus;
import dev.auctoritas.common.enums.OrganizationStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndUserEmailVerificationServiceTest {
  @Autowired private EntityManager entityManager;
  @Autowired private EndUserEmailVerificationService emailVerificationService;
  @Autowired private EndUserEmailVerificationTokenRepository verificationTokenRepository;
  @Autowired private TokenService tokenService;

  private static final String RAW_API_KEY = "pk_live_test_email_verification";
  private Project project;
  private EndUser user;

  @BeforeEach
  void setUp() {
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-email-verification");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-email-verification");
    project.setSettings(settings);
    settings.setProject(project);
    entityManager.persist(project);

    ApiKey apiKey = new ApiKey();
    apiKey.setProject(project);
    apiKey.setName("Test Key");
    apiKey.setPrefix("pk_live_");
    apiKey.setKeyHash(tokenService.hashToken(RAW_API_KEY));
    apiKey.setStatus(ApiKeyStatus.ACTIVE);
    entityManager.persist(apiKey);

    user = new EndUser();
    user.setProject(project);
    user.setEmail("user@example.com");
    user.setPasswordHash("hash");
    user.setEmailVerified(false);
    entityManager.persist(user);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void verifyEmailMarksUserVerifiedAndTokenUsed() {
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    EndUserEmailVerificationService.EmailVerificationPayload payload =
        emailVerificationService.issueVerificationToken(managedUser);

    entityManager.flush();
    entityManager.clear();

    EndUserEmailVerificationResponse response =
        emailVerificationService.verifyEmail(
            RAW_API_KEY, new EndUserEmailVerificationRequest(payload.token(), payload.code()));
    assertThat(response.message()).contains("verified");

    entityManager.flush();
    entityManager.clear();

    EndUser updatedUser = entityManager.find(EndUser.class, user.getId());
    assertThat(updatedUser.getEmailVerified()).isTrue();

    List<EndUserEmailVerificationToken> tokens = verificationTokenRepository.findAll();
    assertThat(tokens).hasSize(1);
    assertThat(tokens.getFirst().getUsedAt()).isNotNull();
  }

  @Test
  void verifyEmailRejectsReusedToken() {
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    EndUserEmailVerificationService.EmailVerificationPayload payload =
        emailVerificationService.issueVerificationToken(managedUser);

    entityManager.flush();
    entityManager.clear();

    emailVerificationService.verifyEmail(
        RAW_API_KEY, new EndUserEmailVerificationRequest(payload.token(), payload.code()));

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                emailVerificationService.verifyEmail(
                    RAW_API_KEY,
                    new EndUserEmailVerificationRequest(payload.token(), payload.code())))
        .hasMessageContaining("verification_token_used");
  }

  @Test
  void verifyEmailRejectsExpiredToken() {
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    EndUserEmailVerificationService.EmailVerificationPayload payload =
        emailVerificationService.issueVerificationToken(managedUser);

    EndUserEmailVerificationToken token =
        verificationTokenRepository
            .findByTokenHash(tokenService.hashToken(payload.token()))
            .orElseThrow();
    token.setExpiresAt(Instant.now().minusSeconds(5));
    verificationTokenRepository.save(token);

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                emailVerificationService.verifyEmail(
                    RAW_API_KEY,
                    new EndUserEmailVerificationRequest(payload.token(), payload.code())))
        .hasMessageContaining("verification_token_expired");
  }

  @Test
  void verifyEmailRejectsMismatchedCodeForToken() {
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    EndUserEmailVerificationService.EmailVerificationPayload payload =
        emailVerificationService.issueVerificationToken(managedUser);

    EndUser otherUser = new EndUser();
    otherUser.setProject(project);
    otherUser.setEmail("other@example.com");
    otherUser.setPasswordHash("hash");
    otherUser.setEmailVerified(false);
    entityManager.persist(otherUser);

    entityManager.flush();
    entityManager.clear();

    EndUserEmailVerificationService.EmailVerificationPayload otherPayload =
        emailVerificationService.issueVerificationToken(entityManager.find(EndUser.class, otherUser.getId()));

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                emailVerificationService.verifyEmail(
                    RAW_API_KEY,
                    new EndUserEmailVerificationRequest(payload.token(), otherPayload.code())))
        .hasMessageContaining("verification_code_invalid");
  }
}
