package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.api.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.api.EndUserResendVerificationRequest;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.messaging.DomainEventPublisher;
import dev.auctoritas.auth.messaging.EmailVerificationResentEvent;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(EndUserEmailVerificationServiceTest.TestDomainEventConfig.class)
class EndUserEmailVerificationServiceTest {
  @Autowired private EntityManager entityManager;
  @Autowired private EndUserEmailVerificationService emailVerificationService;
  @Autowired private EndUserEmailVerificationTokenRepository verificationTokenRepository;
  @Autowired private TokenService tokenService;
  @Autowired private CapturingDomainEventPublisher domainEventPublisher;

  private static final String RAW_API_KEY = "pk_live_test_email_verification";
  private Project project;
  private EndUser user;

  @BeforeEach
  void setUp() {
    domainEventPublisher.clear();
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

  @Test
  void resendVerificationReturnsGenericSuccessForMissingEmail() {
    EndUserEmailVerificationResponse response =
        emailVerificationService.resendVerification(
            RAW_API_KEY, new EndUserResendVerificationRequest("missing@example.com"));
    assertThat(response.message()).contains("verification instructions");
    assertThat(verificationTokenRepository.findAll()).isEmpty();
    assertThat(domainEventPublisher.events()).isEmpty();
  }

  @Test
  void resendVerificationInvalidatesExistingTokensAndPublishesEvent() {
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    emailVerificationService.issueVerificationToken(managedUser);

    entityManager.flush();
    entityManager.clear();

    EndUserEmailVerificationResponse response =
        emailVerificationService.resendVerification(
            RAW_API_KEY, new EndUserResendVerificationRequest(user.getEmail()));
    assertThat(response.message()).contains("verification instructions");

    entityManager.flush();
    entityManager.clear();

    List<EndUserEmailVerificationToken> tokens = verificationTokenRepository.findAll();
    assertThat(tokens).hasSize(2);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() == null).count()).isEqualTo(1);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() != null).count()).isEqualTo(1);

    assertThat(domainEventPublisher.events()).hasSize(1);
    CapturingDomainEventPublisher.PublishedEvent published = domainEventPublisher.events().getFirst();
    assertThat(published.eventType()).isEqualTo(EmailVerificationResentEvent.EVENT_TYPE);
    assertThat(published.payload()).isInstanceOf(EmailVerificationResentEvent.class);
    EmailVerificationResentEvent event = (EmailVerificationResentEvent) published.payload();
    assertThat(event.projectId()).isEqualTo(project.getId());
    assertThat(event.userId()).isEqualTo(user.getId());
    assertThat(event.email()).isEqualTo(user.getEmail());
    assertThat(event.emailVerificationTokenId()).isNotNull();
    assertThat(event.emailVerificationExpiresAt()).isNotNull();
  }

  @Test
  void resendVerificationEnforcesPerUserRateLimit() {
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    emailVerificationService.issueVerificationToken(managedUser);
    emailVerificationService.issueVerificationToken(managedUser);
    emailVerificationService.issueVerificationToken(managedUser);

    entityManager.flush();
    entityManager.clear();

    emailVerificationService.resendVerification(
        RAW_API_KEY, new EndUserResendVerificationRequest(user.getEmail()));

    entityManager.flush();
    entityManager.clear();

    List<EndUserEmailVerificationToken> tokens = verificationTokenRepository.findAll();
    assertThat(tokens).hasSize(3);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() == null).count()).isEqualTo(1);
    assertThat(domainEventPublisher.events()).isEmpty();
  }

  static class CapturingDomainEventPublisher implements DomainEventPublisher {
    private final java.util.List<PublishedEvent> published = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    public void publish(String eventType, Object payload) {
      published.add(new PublishedEvent(eventType, payload));
    }

    java.util.List<PublishedEvent> events() {
      return java.util.List.copyOf(published);
    }

    void clear() {
      published.clear();
    }

    record PublishedEvent(String eventType, Object payload) {}
  }

  @TestConfiguration
  static class TestDomainEventConfig {
    @Bean
    @Primary
    CapturingDomainEventPublisher domainEventPublisher() {
      return new CapturingDomainEventPublisher();
    }
  }
}
