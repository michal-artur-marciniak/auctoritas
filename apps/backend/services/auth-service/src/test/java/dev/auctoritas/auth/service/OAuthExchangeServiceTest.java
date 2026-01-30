package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.auctoritas.auth.api.EndUserLoginResponse;
import dev.auctoritas.auth.api.OAuthExchangeRequest;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.oauth.OAuthExchangeCode;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import dev.auctoritas.auth.shared.enums.OrganizationStatus;
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
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-oauth-exchange");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);

    ProjectSettings settings = new ProjectSettings();

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-oauth-exchange");
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
  void exchangeIssuesTokensAndMarksCodeUsedAndRejectsReplay() {
    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("user@example.com");
    user.setName("Test User");
    user.setEmailVerified(true);
    user.setPasswordHash("password-hash");
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
    EndUser user = new EndUser();
    user.setProject(entityManager.find(Project.class, project.getId()));
    user.setEmail("expired@example.com");
    user.setEmailVerified(true);
    user.setPasswordHash("password-hash");
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
