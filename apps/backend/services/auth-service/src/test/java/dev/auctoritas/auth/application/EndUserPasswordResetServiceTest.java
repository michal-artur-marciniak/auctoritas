package dev.auctoritas.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.interface.api.EndUserPasswordForgotRequest;
import dev.auctoritas.auth.interface.api.EndUserPasswordResetRequest;
import dev.auctoritas.auth.interface.api.EndUserPasswordResetResponse;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordResetToken;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.enduser.EndUserSession;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.infrastructure.messaging.DomainEventPublisher;
import dev.auctoritas.auth.application.event.PasswordResetRequestedEvent;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserPasswordHistoryRepository;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserPasswordResetTokenRepository;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.infrastructure.persistence.repository.EndUserSessionRepository;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.project.Slug;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndUserPasswordResetServiceTest {

  @TestConfiguration
  static class Config {
    @Bean
    @Primary
    InMemoryDomainEventPublisher inMemoryDomainEventPublisher() {
      return new InMemoryDomainEventPublisher();
    }
  }

  static final class InMemoryDomainEventPublisher implements DomainEventPublisher {
    record PublishedEvent(String type, Object payload) {}

    private final List<PublishedEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void publish(String eventType, Object payload) {
      events.add(new PublishedEvent(eventType, payload));
    }

    List<PublishedEvent> events() {
      return events;
    }
  }

  @Autowired private EntityManager entityManager;
  @Autowired private EndUserPasswordResetService passwordResetService;
  @Autowired private EndUserPasswordResetTokenRepository resetTokenRepository;
  @Autowired private EndUserPasswordHistoryRepository passwordHistoryRepository;
  @Autowired private EndUserRefreshTokenRepository refreshTokenRepository;
  @Autowired private EndUserSessionRepository sessionRepository;
  @Autowired private TokenService tokenService;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private InMemoryDomainEventPublisher domainEventPublisher;

  private static final String RAW_API_KEY = "pk_live_test_password_reset";
  private Project project;
  private EndUser user;

  @BeforeEach
  void setUp() {
    domainEventPublisher.events().clear();

    Organization org = Organization.create("Test Org", Slug.of("test-org-password-reset"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-password-reset"));
    entityManager.persist(project);

    ApiKey apiKey = ApiKey.create(
        project,
        "Test Key",
        "pk_live_",
        tokenService.hashToken(RAW_API_KEY));
    entityManager.persist(apiKey);

    user = EndUser.create(project, Email.of("user@example.com"), Password.fromHash(passwordEncoder.encode("UserPass123!")), null);
    entityManager.persist(user);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void requestResetDoesNotLeakTokenAndPublishesEvent() {
    EndUserPasswordResetResponse response =
        passwordResetService.requestReset(
            RAW_API_KEY,
            new EndUserPasswordForgotRequest("user@example.com"),
            "1.2.3.4",
            "test-agent");

    assertThat(response.message()).contains("If an account exists");
    assertThat(response.resetToken()).isNull();

    List<EndUserPasswordResetToken> tokens = resetTokenRepository.findAll();
    assertThat(tokens).hasSize(1);
    EndUserPasswordResetToken token = tokens.getFirst();
    assertThat(token.getUser().getId()).isNotNull();
    assertThat(token.getProject().getId()).isNotNull();
    assertThat(token.getTokenHash()).isNotBlank();
    assertThat(token.getUsedAt()).isNull();
    assertThat(token.getIpAddress()).isEqualTo("1.2.3.4");
    assertThat(token.getUserAgent()).isEqualTo("test-agent");

    InMemoryDomainEventPublisher.PublishedEvent published = domainEventPublisher.events().stream()
        .filter(event -> PasswordResetRequestedEvent.EVENT_TYPE.equals(event.type()))
        .findFirst()
        .orElseThrow();
    assertThat(published.payload()).isInstanceOf(PasswordResetRequestedEvent.class);
    PasswordResetRequestedEvent event = (PasswordResetRequestedEvent) published.payload();
    assertThat(event.projectId()).isEqualTo(project.getId());
    assertThat(event.userId()).isEqualTo(user.getId());
    assertThat(event.email()).isEqualTo("user@example.com");
    assertThat(event.resetTokenId()).isNotNull();
    assertThat(event.resetTokenHash()).isNotBlank();
    assertThat(event.expiresAt()).isNotNull();

    assertThat(event.resetTokenId()).isEqualTo(token.getId());
    assertThat(event.resetTokenHash()).isEqualTo(token.getTokenHash());
  }

  @Test
  void requestResetInvalidatesExistingTokensForSameUserAndProject() {
    passwordResetService.requestReset(
        RAW_API_KEY, new EndUserPasswordForgotRequest("user@example.com"), null, null);

    entityManager.flush();
    entityManager.clear();

    passwordResetService.requestReset(
        RAW_API_KEY, new EndUserPasswordForgotRequest("user@example.com"), null, null);

    List<EndUserPasswordResetToken> tokens = resetTokenRepository.findAll();
    assertThat(tokens).hasSize(2);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() == null)).hasSize(1);
    assertThat(tokens.stream().filter(t -> t.getUsedAt() != null)).hasSize(1);

    long eventCount = domainEventPublisher.events().stream()
        .filter(event -> PasswordResetRequestedEvent.EVENT_TYPE.equals(event.type()))
        .count();
    assertThat(eventCount).isEqualTo(2);
  }

  @Test
  void requestResetUnknownEmailDoesNotCreateTokenOrPublishEvent() {
    EndUserPasswordResetResponse response =
        passwordResetService.requestReset(
            RAW_API_KEY, new EndUserPasswordForgotRequest("missing@example.com"), null, null);

    assertThat(response.message()).contains("If an account exists");
    assertThat(response.resetToken()).isNull();
    assertThat(resetTokenRepository.findAll()).isEmpty();
    assertThat(domainEventPublisher.events().stream()
        .anyMatch(event -> PasswordResetRequestedEvent.EVENT_TYPE.equals(event.type())))
        .isFalse();
  }

  @Test
  void resetPasswordUpdatesPasswordRevokesSessionsAndMarksTokenUsed() {
    UUID userId = user.getId();
    EndUser managedUser = entityManager.find(EndUser.class, userId);
    Project managedProject = entityManager.find(Project.class, project.getId());
    String previousPasswordHash = managedUser.getPasswordHash();

    String rawResetToken = "reset-token-1";
    EndUserPasswordResetToken token = new EndUserPasswordResetToken();
    token.setProject(managedProject);
    token.setUser(managedUser);
    token.setTokenHash(tokenService.hashToken(rawResetToken));
    token.setExpiresAt(Instant.now().plusSeconds(3600));
    entityManager.persist(token);

    EndUserRefreshToken refreshToken = EndUserRefreshToken.create(
        managedUser,
        tokenService.hashToken("refresh-token-1"),
        Duration.ofHours(1),
        null,
        null);
    entityManager.persist(refreshToken);

    EndUserSession session = EndUserSession.create(
        managedUser,
        "1.2.3.4",
        Map.of("userAgent", "test"),
        Duration.ofHours(1));
    entityManager.persist(session);

    entityManager.flush();
    entityManager.clear();

    EndUserPasswordResetResponse response =
        passwordResetService.resetPassword(
            RAW_API_KEY, new EndUserPasswordResetRequest(rawResetToken, "NewPass123!"));

    assertThat(response.message()).contains("Password");

    entityManager.flush();
    entityManager.clear();

    EndUser updatedUser = entityManager.find(EndUser.class, userId);
    assertThat(passwordEncoder.matches("NewPass123!", updatedUser.getPasswordHash())).isTrue();

    EndUserPasswordResetToken updatedToken = resetTokenRepository.findAll().getFirst();
    assertThat(updatedToken.getUsedAt()).isNotNull();

    List<EndUserPasswordHistory> history = passwordHistoryRepository.findAll();
    assertThat(history).hasSize(1);
    assertThat(history.getFirst().getPasswordHash()).isEqualTo(previousPasswordHash);

    List<EndUserRefreshToken> refreshTokens = refreshTokenRepository.findAll();
    assertThat(refreshTokens).hasSize(1);
    assertThat(refreshTokens.getFirst().isRevoked()).isTrue();
    assertThat(sessionRepository.findByUserId(userId)).isEmpty();

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                passwordResetService.resetPassword(
                    RAW_API_KEY,
                    new EndUserPasswordResetRequest(rawResetToken, "OtherPass123!")))
        .hasMessageContaining("reset_token_used");
  }

  @Test
  void resetPasswordRejectsPasswordReuseFromHistory() {
    String rawResetToken = "reset-token-2";
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    Project managedProject = entityManager.find(Project.class, project.getId());

    EndUserPasswordResetToken token = new EndUserPasswordResetToken();
    token.setProject(managedProject);
    token.setUser(managedUser);
    token.setTokenHash(tokenService.hashToken(rawResetToken));
    token.setExpiresAt(Instant.now().plusSeconds(3600));
    entityManager.persist(token);

    EndUserPasswordHistory history = new EndUserPasswordHistory();
    history.setProject(managedProject);
    history.setUser(managedUser);
    history.setPasswordHash(passwordEncoder.encode("OldPass123!"));
    entityManager.persist(history);

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                passwordResetService.resetPassword(
                    RAW_API_KEY,
                    new EndUserPasswordResetRequest(rawResetToken, "OldPass123!")))
        .hasMessageContaining("password_reuse_not_allowed");
  }

  @Test
  void resetPasswordRejectsExpiredToken() {
    String rawResetToken = "reset-token-3";
    EndUser managedUser = entityManager.find(EndUser.class, user.getId());
    Project managedProject = entityManager.find(Project.class, project.getId());

    EndUserPasswordResetToken token = new EndUserPasswordResetToken();
    token.setProject(managedProject);
    token.setUser(managedUser);
    token.setTokenHash(tokenService.hashToken(rawResetToken));
    token.setExpiresAt(Instant.now().minusSeconds(5));
    entityManager.persist(token);

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                passwordResetService.resetPassword(
                    RAW_API_KEY,
                    new EndUserPasswordResetRequest(rawResetToken, "NewPass123!")))
        .hasMessageContaining("reset_token_expired");
  }
}
