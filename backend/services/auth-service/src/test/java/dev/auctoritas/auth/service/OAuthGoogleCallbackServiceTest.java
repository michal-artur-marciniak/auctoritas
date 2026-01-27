package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.OAuthAuthorizationRequestRepository;
import dev.auctoritas.auth.repository.OAuthConnectionRepository;
import dev.auctoritas.auth.repository.OAuthExchangeCodeRepository;
import dev.auctoritas.common.enums.OrganizationStatus;
import dev.auctoritas.common.enums.ProjectStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaConfig.class, TokenService.class, OAuthGoogleCallbackService.class, OAuthGoogleCallbackServiceTest.TestConfig.class})
class OAuthGoogleCallbackServiceTest {

  @org.springframework.beans.factory.annotation.Autowired private EntityManager entityManager;
  @org.springframework.beans.factory.annotation.Autowired private TokenService tokenService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthGoogleCallbackService callbackService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository;
  @org.springframework.beans.factory.annotation.Autowired private EndUserRepository endUserRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthConnectionRepository oauthConnectionRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthExchangeCodeRepository oauthExchangeCodeRepository;
  @org.springframework.beans.factory.annotation.Autowired private StubGoogleOAuthClient stubGoogleOAuthClient;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-oauth-callback");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);
    entityManager.flush();

    ProjectSettings settings = new ProjectSettings();
    Map<String, Object> google = new HashMap<>();
    google.put("enabled", true);
    google.put("clientId", "google-client-id");

    Map<String, Object> oauthConfig = new HashMap<>();
    oauthConfig.put("google", google);
    settings.setOauthConfig(oauthConfig);
    settings.setOauthGoogleClientSecretEnc("google-client-secret");

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-oauth-callback");
    project.setStatus(ProjectStatus.ACTIVE);
    project.setSettings(settings);
    settings.setProject(project);

    entityManager.persist(project);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should exchange provider code, create/link user, and issue one-time exchange code")
  void shouldHandleCallbackAndIssueExchangeCode() {
    String state = "state-123";
    OAuthAuthorizationRequest authRequest = new OAuthAuthorizationRequest();
    authRequest.setProject(project);
    authRequest.setProvider("google");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubGoogleOAuthClient.userInfoRef.set(
        new GoogleOAuthClient.GoogleUserInfo("google-sub-1", "User@Example.com", true, "Test User"));

    String redirectUrl =
        callbackService.handleCallback(
            "provider-code",
            state,
            "https://gateway.example.com/api/v1/auth/oauth/google/callback");

    assertThat(redirectUrl).startsWith("https://example.com/app/callback");
    assertThat(redirectUrl).contains("auctoritas_code=");

    // state is consumed
    assertThat(oauthAuthorizationRequestRepository.findByStateHash(tokenService.hashToken(state)))
        .isEmpty();

    // user created and verified
    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.getEmailVerified()).isTrue();
              assertThat(user.getPasswordHash()).isNotBlank();
            });

    // oauth connection created
    assertThat(
            oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
                project.getId(), "google", "google-sub-1"))
        .isPresent();

    // exchange code persisted
    String rawCode =
        UriComponentsBuilder.fromUriString(redirectUrl)
            .build(true)
            .getQueryParams()
            .getFirst("auctoritas_code");
    assertThat(rawCode).isNotBlank();

    String codeHash = tokenService.hashToken(rawCode);
    assertThat(oauthExchangeCodeRepository.findByCodeHash(codeHash))
        .isPresent()
        .get()
        .satisfies(
            code -> {
              assertThat(code.getUsedAt()).isNull();
              assertThat(code.getExpiresAt()).isAfter(Instant.now());
            });
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    GoogleOAuthClient googleOAuthClient() {
      return new StubGoogleOAuthClient();
    }

    @Bean
    @Primary
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    @Bean
    @Primary
    TextEncryptor oauthClientSecretEncryptor() {
      return Encryptors.noOpText();
    }
  }

  static class StubGoogleOAuthClient implements GoogleOAuthClient {
    final AtomicReference<GoogleTokenExchangeRequest> exchangeRequestRef = new AtomicReference<>();
    final AtomicReference<GoogleUserInfo> userInfoRef = new AtomicReference<>();

    @Override
    public GoogleTokenResponse exchangeAuthorizationCode(GoogleTokenExchangeRequest request) {
      exchangeRequestRef.set(request);
      return new GoogleTokenResponse("access-token", null, null, "Bearer", 3600L, "openid email profile");
    }

    @Override
    public GoogleUserInfo fetchUserInfo(String accessToken) {
      GoogleUserInfo info = userInfoRef.get();
      if (info == null) {
        return new GoogleUserInfo("google-sub-1", "user@example.com", true, "Test User");
      }
      return info;
    }
  }
}
