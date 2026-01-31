package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserRefreshRequest;
import dev.auctoritas.auth.api.EndUserRefreshResponse;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.domain.model.project.ApiKeyStatus;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.domain.model.enduser.Password;
import dev.auctoritas.auth.domain.model.project.Slug;
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
class EndUserRefreshServiceTest {
  private static final String RAW_API_KEY = "pk_live_test_refresh";

  @Autowired private EntityManager entityManager;
  @Autowired private EndUserRefreshService refreshService;
  @Autowired private EndUserRefreshTokenRepository refreshTokenRepository;
  @Autowired private TokenService tokenService;
  @Autowired private JwtService jwtService;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = Organization.create("Test Org", Slug.of("test-org-refresh"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-refresh"));
    entityManager.persist(project);

    ApiKey apiKey = new ApiKey();
    apiKey.setProject(project);
    apiKey.setName("Test Key");
    apiKey.setPrefix("pk_live_");
    apiKey.setKeyHash(tokenService.hashToken(RAW_API_KEY));
    apiKey.setStatus(ApiKeyStatus.ACTIVE);
    entityManager.persist(apiKey);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void refreshAllowsUnverifiedUserByDefaultAndIncludesEmailVerifiedClaim() {
    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("user@example.com"),
        Password.fromHash("hash"),
        null);
    entityManager.persist(user);

    String rawRefreshToken = "refresh-raw";
    EndUserRefreshToken token = new EndUserRefreshToken();
    token.setUser(user);
    token.setTokenHash(tokenService.hashToken(rawRefreshToken));
    token.setExpiresAt(Instant.now().plusSeconds(3600));
    token.setRevoked(false);
    entityManager.persist(token);

    entityManager.flush();
    entityManager.clear();

    EndUserRefreshResponse response =
        refreshService.refresh(
            RAW_API_KEY,
            new EndUserRefreshRequest(rawRefreshToken),
            "1.2.3.4",
            "test-agent");

    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();

    JwtService.JwtValidationResult validation = jwtService.validateToken(response.accessToken());
    assertThat(validation.valid()).isTrue();
    assertThat(validation.claims().get(JwtService.CLAIM_EMAIL_VERIFIED, Boolean.class)).isEqualTo(false);

    List<EndUserRefreshToken> tokens = refreshTokenRepository.findAll();
    assertThat(tokens).hasSize(2);
    assertThat(tokens.stream().filter(EndUserRefreshToken::isRevoked).count()).isEqualTo(1);
    assertThat(
            tokens.stream()
                .anyMatch(
                    t ->
                        t.getTokenHash().equals(tokenService.hashToken(response.refreshToken()))
                            && !t.isRevoked()))
        .isTrue();
  }

  @Test
  void refreshRejectsUnverifiedUserWhenProjectRequiresVerifiedEmail() {
    ProjectSettings settings = entityManager.find(Project.class, project.getId()).getSettings();
    settings.updateAuthSettings(true);
    entityManager.flush();

    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("user2@example.com"),
        Password.fromHash("hash"),
        null);
    entityManager.persist(user);

    String rawRefreshToken = "refresh-raw-2";
    EndUserRefreshToken token = new EndUserRefreshToken();
    token.setUser(user);
    token.setTokenHash(tokenService.hashToken(rawRefreshToken));
    token.setExpiresAt(Instant.now().plusSeconds(3600));
    token.setRevoked(false);
    entityManager.persist(token);

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () ->
                refreshService.refresh(
                    RAW_API_KEY,
                    new EndUserRefreshRequest(rawRefreshToken),
                    null,
                    null))
        .hasMessageContaining("email_not_verified");

    List<EndUserRefreshToken> tokens = refreshTokenRepository.findAll();
    assertThat(tokens).hasSize(1);
    assertThat(tokens.getFirst().isRevoked()).isFalse();
    assertThat(tokens.getFirst().getReplacedBy()).isNull();
  }
}
