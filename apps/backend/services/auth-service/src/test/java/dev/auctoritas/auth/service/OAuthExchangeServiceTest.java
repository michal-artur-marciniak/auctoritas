package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.api.OAuthExchangeRequest;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.enduser.Password;
import dev.auctoritas.auth.domain.project.Slug;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OAuthExchangeServiceTest {
  private static final String RAW_API_KEY = "pk_live_test_oauth_exchange";

  @Autowired private EntityManager entityManager;
  @Autowired private OAuthExchangeService exchangeService;
  @Autowired private TokenService tokenService;
  @Autowired private JwtService jwtService;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = Organization.create("Test Org", Slug.of("test-org-oauth-exchange"));
    entityManager.persist(org);

    project = Project.create(org, "Test Project", Slug.of("test-project-oauth-exchange"));
    entityManager.persist(project);

    ApiKey apiKey = ApiKey.create(
        project,
        "Test Key",
        "pk_live_",
        tokenService.hashToken(RAW_API_KEY));
    entityManager.persist(apiKey);

    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void exchangeIssuesTokensAndMarksCodeUsedAndRejectsReplay() {
    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("user@example.com"),
        Password.fromHash("password-hash"),
        "Test User");
    user.verifyEmail();
    entityManager.persist(user);

    String rawCode = tokenService.generateOAuthExchangeCode();
    OAuthExchangeCode exchangeCode = new OAuthExchangeCode();
    exchangeCode.setProject(entityManager.find(Project.class, project.getId()));
    exchangeCode.setUser(user);
    exchangeCode.setProvider("google");
    exchangeCode.setCodeHash(tokenService.hashToken(rawCode));
    exchangeCode.setExpiresAt(Instant.now().plusSeconds(60));
    entityManager.persist(exchangeCode);

    entityManager.flush();
    entityManager.clear();

    EndUserLoginResponse response =
        exchangeService.exchange(
            RAW_API_KEY, new OAuthExchangeRequest(rawCode), "1.2.3.4", "test-agent");

    assertThat(response.user().email()).isEqualTo("user@example.com");
    assertThat(response.accessToken()).isNotBlank();
    assertThat(response.refreshToken()).isNotBlank();

    JwtService.JwtValidationResult validation = jwtService.validateToken(response.accessToken());
    assertThat(validation.valid()).isTrue();
    assertThat(validation.claims().get(JwtService.CLAIM_EMAIL_VERIFIED, Boolean.class)).isEqualTo(true);

    entityManager.flush();
    entityManager.clear();

    OAuthExchangeCode persisted = entityManager.find(OAuthExchangeCode.class, exchangeCode.getId());
    assertThat(persisted.getUsedAt()).isNotNull();

    assertThatThrownBy(
            () ->
                exchangeService.exchange(
                    RAW_API_KEY, new OAuthExchangeRequest(rawCode), null, null))
        .hasMessageContaining("invalid_oauth_code");
  }

  @Test
  void exchangeRejectsExpiredCode() {
    Project managedProject = entityManager.find(Project.class, project.getId());
    EndUser user = EndUser.create(
        managedProject,
        Email.of("expired@example.com"),
        Password.fromHash("password-hash"),
        null);
    user.verifyEmail();
    entityManager.persist(user);

    String rawCode = tokenService.generateOAuthExchangeCode();
    OAuthExchangeCode exchangeCode = new OAuthExchangeCode();
    exchangeCode.setProject(entityManager.find(Project.class, project.getId()));
    exchangeCode.setUser(user);
    exchangeCode.setProvider("google");
    exchangeCode.setCodeHash(tokenService.hashToken(rawCode));
    exchangeCode.setExpiresAt(Instant.now().minusSeconds(5));
    entityManager.persist(exchangeCode);

    entityManager.flush();
    entityManager.clear();

    assertThatThrownBy(
            () -> exchangeService.exchange(RAW_API_KEY, new OAuthExchangeRequest(rawCode), null, null))
        .hasMessageContaining("invalid_oauth_code");
  }
}
