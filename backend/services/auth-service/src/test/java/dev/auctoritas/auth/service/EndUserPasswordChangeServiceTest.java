package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserPasswordChangeRequest;
import dev.auctoritas.auth.api.EndUserPasswordChangeResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserPasswordHistory;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.enduser.EndUserSession;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserPasswordHistoryRepository;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import dev.auctoritas.auth.security.EndUserPrincipal;
import dev.auctoritas.common.enums.ApiKeyStatus;
import dev.auctoritas.common.enums.OrganizationStatus;
import jakarta.persistence.EntityManager;
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
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-password-change");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-password-change");
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
    user.setPasswordHash(passwordEncoder.encode("UserPass123!"));
    entityManager.persist(user);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void changePasswordUpdatesPasswordRecordsHistoryAndRevokesOtherSessions() {
    UUID userId = user.getId();
    Instant now = Instant.now();

    EndUser managedUser = entityManager.find(EndUser.class, userId);

    EndUserSession oldSession = new EndUserSession();
    oldSession.setUser(managedUser);
    oldSession.setDeviceInfo(Map.of("userAgent", "old"));
    oldSession.setIpAddress("1.2.3.4");
    oldSession.setExpiresAt(now.plusSeconds(3600));
    oldSession.setCreatedAt(now.minusSeconds(200));
    entityManager.persist(oldSession);

    EndUserSession currentSession = new EndUserSession();
    currentSession.setUser(managedUser);
    currentSession.setDeviceInfo(Map.of("userAgent", "current"));
    currentSession.setIpAddress("1.2.3.4");
    currentSession.setExpiresAt(now.plusSeconds(3600));
    currentSession.setCreatedAt(now.minusSeconds(10));
    entityManager.persist(currentSession);

    EndUserRefreshToken oldToken = new EndUserRefreshToken();
    oldToken.setUser(managedUser);
    oldToken.setTokenHash(tokenService.hashToken("refresh-old"));
    oldToken.setExpiresAt(now.plusSeconds(3600));
    oldToken.setRevoked(false);
    oldToken.setCreatedAt(now.minusSeconds(300));
    entityManager.persist(oldToken);

    EndUserRefreshToken currentToken = new EndUserRefreshToken();
    currentToken.setUser(managedUser);
    currentToken.setTokenHash(tokenService.hashToken("refresh-current"));
    currentToken.setExpiresAt(now.plusSeconds(3600));
    currentToken.setRevoked(false);
    currentToken.setCreatedAt(now.minusSeconds(5));
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
}
