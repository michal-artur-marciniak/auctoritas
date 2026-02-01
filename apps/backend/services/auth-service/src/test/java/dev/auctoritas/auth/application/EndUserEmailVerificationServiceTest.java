package dev.auctoritas.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.api.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.api.EndUserResendVerificationRequest;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.infrastructure.messaging.DomainEventPublisher;
import dev.auctoritas.auth.application.event.EmailVerificationResentEvent;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserEmailVerificationTokenRepository;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.project.Slug;
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
    Organization org = Organization.create("Test Org", Slug.of("test-org-email-verification"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-email-verification"));
    entityManager.persist(project);

    ApiKey apiKey = ApiKey.create(
        project,
        "Test Key",
        "pk_live_",
        tokenService.hashToken(RAW_API_KEY));
    entityManager.persist(apiKey);

    user = EndUser.create(project, Email.of("user@example.com"), Password.fromHash("hash"), null);
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
    assertThat(updatedUser.isEmailVerified()).isTrue();

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

    EndUser otherUser = EndUser.create(project, Email.of("other@example.com"), Password.fromHash("hash"), null);
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
    assertThat(domainEventPublisher.events().stream()
        .anyMatch(event -> EmailVerificationResentEvent.EVENT_TYPE.equals(event.eventType())))
        .isFalse();
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

    CapturingDomainEventPublisher.PublishedEvent published = domainEventPublisher.events().stream()
        .filter(event -> EmailVerificationResentEvent.EVENT_TYPE.equals(event.eventType()))
        .findFirst()
        .orElseThrow();
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
    assertThat(domainEventPublisher.events().stream()
        .anyMatch(event -> EmailVerificationResentEvent.EVENT_TYPE.equals(event.eventType())))
        .isFalse();
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
