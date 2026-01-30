package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserRefreshRequest;
import dev.auctoritas.auth.api.EndUserRefreshResponse;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.domain.organization.OrganizationStatus;
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
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-refresh");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-refresh");
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

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void refreshAllowsUnverifiedUserByDefaultAndIncludesEmailVerifiedClaim() {
    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("user@example.com");
    user.setPasswordHash("hash");
    user.setEmailVerified(false);
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
    settings.setRequireVerifiedEmailForLogin(true);
    entityManager.flush();

    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("user2@example.com");
    user.setPasswordHash("hash");
    user.setEmailVerified(false);
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
