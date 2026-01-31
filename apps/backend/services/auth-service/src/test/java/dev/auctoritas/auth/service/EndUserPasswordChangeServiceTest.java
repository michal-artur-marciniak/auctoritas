package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserPasswordChangeRequest;
import dev.auctoritas.auth.api.EndUserPasswordChangeResponse;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.enduser.EndUserSession;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserPasswordHistoryRepository;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import dev.auctoritas.auth.security.EndUserPrincipal;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.project.Slug;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EndUserPasswordChangeServiceTest {
  private static final String RAW_API_KEY = "pk_live_test_password_change";

  @Autowired private EntityManager entityManager;
  @Autowired private EndUserPasswordChangeService passwordChangeService;
  @Autowired private EndUserPasswordHistoryRepository passwordHistoryRepository;
  @Autowired private EndUserSessionRepository sessionRepository;
  @Autowired private EndUserRefreshTokenRepository refreshTokenRepository;
  @Autowired private TokenService tokenService;
  @Autowired private PasswordEncoder passwordEncoder;

  private Project project;
  private EndUser user;

  @BeforeEach
  void setUp() {
    Organization org = Organization.create("Test Org", Slug.of("test-org-password-change"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-password-change"));
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
  void changePasswordUpdatesPasswordRecordsHistoryAndRevokesOtherSessions() {
    UUID userId = user.getId();
    Instant now = Instant.now();

    EndUser managedUser = entityManager.find(EndUser.class, userId);

    EndUserSession oldSession = EndUserSession.create(
        managedUser,
        "1.2.3.4",
        Map.of("userAgent", "old"),
        Duration.ofHours(1));
    setCreatedAt(oldSession, now.minusSeconds(200));
    entityManager.persist(oldSession);

    EndUserSession currentSession = EndUserSession.create(
        managedUser,
        "1.2.3.4",
        Map.of("userAgent", "current"),
        Duration.ofHours(1));
    setCreatedAt(currentSession, now.minusSeconds(10));
    entityManager.persist(currentSession);

    EndUserRefreshToken oldToken = EndUserRefreshToken.create(
        managedUser,
        tokenService.hashToken("refresh-old"),
        Duration.ofHours(1),
        null,
        null);
    setCreatedAt(oldToken, now.minusSeconds(300));
    entityManager.persist(oldToken);

    EndUserRefreshToken currentToken = EndUserRefreshToken.create(
        managedUser,
        tokenService.hashToken("refresh-current"),
        Duration.ofHours(1),
        null,
        null);
    setCreatedAt(currentToken, now.minusSeconds(5));
    entityManager.persist(currentToken);

    entityManager.flush();
    UUID currentSessionId = currentSession.getId();
    UUID currentTokenId = currentToken.getId();
    String previousPasswordHash = managedUser.getPasswordHash();
    entityManager.clear();

    EndUserPasswordChangeResponse response =
        passwordChangeService.changePassword(
            RAW_API_KEY,
            new EndUserPrincipal(userId, project.getId(), "user@example.com"),
            currentSessionId,
            new EndUserPasswordChangeRequest("UserPass123!", "NewPass123!"));

    assertThat(response.message()).isEqualTo("Password changed");
    assertThat(response.keptCurrentSession()).isTrue();
    assertThat(response.revokedOtherSessions()).isTrue();

    entityManager.flush();
    entityManager.clear();

    EndUser updatedUser = entityManager.find(EndUser.class, userId);
    assertThat(passwordEncoder.matches("NewPass123!", updatedUser.getPasswordHash())).isTrue();

    List<EndUserPasswordHistory> history = passwordHistoryRepository.findAll();
    assertThat(history).hasSize(1);
    assertThat(history.getFirst().getPasswordHash()).isEqualTo(previousPasswordHash);

    List<EndUserSession> sessions = sessionRepository.findByUserId(userId);
    assertThat(sessions).hasSize(1);
    assertThat(sessions.getFirst().getId()).isEqualTo(currentSessionId);

    List<EndUserRefreshToken> tokens = refreshTokenRepository.findAll();
    assertThat(tokens).hasSize(2);
    assertThat(tokens.stream().filter(t -> t.getId().equals(currentTokenId)).findFirst().orElseThrow().isRevoked())
        .isFalse();
    assertThat(tokens.stream().filter(t -> !t.getId().equals(currentTokenId)).findFirst().orElseThrow().isRevoked())
        .isTrue();
  }

  @Test
  void changePasswordRejectsInvalidCurrentPassword() {
    assertThatThrownBy(
            () ->
                passwordChangeService.changePassword(
                    RAW_API_KEY,
                    new EndUserPrincipal(user.getId(), project.getId(), "user@example.com"),
                    null,
                    new EndUserPasswordChangeRequest("WrongPass123!", "NewPass123!")))
        .hasMessageContaining("invalid_current_password");
  }

  @Test
  void changePasswordRejectsPasswordReuseFromHistory() {
    UUID userId = user.getId();
    EndUser managedUser = entityManager.find(EndUser.class, userId);

    EndUserPasswordHistory history = new EndUserPasswordHistory();
    history.setProject(entityManager.find(Project.class, project.getId()));
    history.setUser(managedUser);
    history.setPasswordHash(passwordEncoder.encode("OldPass123!"));
    entityManager.persist(history);

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                passwordChangeService.changePassword(
                    RAW_API_KEY,
                    new EndUserPrincipal(userId, project.getId(), "user@example.com"),
                    null,
                    new EndUserPasswordChangeRequest("UserPass123!", "OldPass123!")))
        .hasMessageContaining("password_reuse_not_allowed");
  }

  private void setCreatedAt(BaseEntity entity, Instant createdAt) {
    try {
      var setCreatedAt = BaseEntity.class.getDeclaredMethod("setCreatedAt", Instant.class);
      setCreatedAt.setAccessible(true);
      setCreatedAt.invoke(entity, createdAt);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Failed to set createdAt", ex);
    }
  }
}
