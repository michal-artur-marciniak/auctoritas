package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.EndUserRepository;
import dev.auctoritas.auth.repository.OAuthAuthorizationRequestRepository;
import dev.auctoritas.auth.repository.OAuthConnectionRepository;
import dev.auctoritas.auth.repository.OAuthExchangeCodeRepository;
import dev.auctoritas.auth.service.oauth.FacebookOAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthAccountLinkingService;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.UriComponentsBuilder;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  JpaConfig.class,
  TokenService.class,
  OAuthProviderRegistry.class,
  FacebookOAuthProvider.class,
  OAuthAccountLinkingService.class,
  OAuthFacebookCallbackService.class,
  OAuthFacebookCallbackServiceTest.TestConfig.class
})
class OAuthFacebookCallbackServiceTest {

  @org.springframework.beans.factory.annotation.Autowired private EntityManager entityManager;
  @org.springframework.beans.factory.annotation.Autowired private TokenService tokenService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthFacebookCallbackService callbackService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository;
  @org.springframework.beans.factory.annotation.Autowired private EndUserRepository endUserRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthConnectionRepository oauthConnectionRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthExchangeCodeRepository oauthExchangeCodeRepository;
  @org.springframework.beans.factory.annotation.Autowired private StubFacebookOAuthClient stubFacebookOAuthClient;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-facebook-oauth-callback");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);
    entityManager.flush();

    ProjectSettings settings = new ProjectSettings();
    Map<String, Object> facebook = new HashMap<>();
    facebook.put("enabled", true);
    facebook.put("clientId", "facebook-client-id");

    Map<String, Object> oauthConfig = new HashMap<>();
    oauthConfig.put("facebook", facebook);
    settings.setOauthConfig(oauthConfig);
    settings.setOauthFacebookClientSecretEnc("facebook-client-secret");

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-facebook-oauth-callback");
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
    authRequest.setProvider("facebook");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubFacebookOAuthClient.userInfoRef.set(
        new FacebookOAuthClient.FacebookUserInfo("fb-123", "Test User", "User@Example.com"));

    String redirectUrl =
        callbackService.handleCallback(
            "provider-code", state, "https://gateway.example.com/api/v1/auth/oauth/facebook/callback");

    assertThat(redirectUrl).startsWith("https://example.com/app/callback");
    assertThat(redirectUrl).contains("auctoritas_code=");

    // state is consumed
    assertThat(oauthAuthorizationRequestRepository.findByStateHash(tokenService.hashToken(state))).isEmpty();

    // user created (email is not asserted as verified by Facebook)
    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.getEmailVerified()).isFalse();
              assertThat(user.getPasswordHash()).isNotBlank();
            });

    // oauth connection created
    assertThat(
            oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
                project.getId(), "facebook", "fb-123"))
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

  @Test
  @DisplayName("Should use existing OAuth connection when provider_user_id matches")
  void shouldUseExistingConnectionByProviderUserId() {
    EndUser existing = new EndUser();
    existing.setProject(project);
    existing.setEmail("user@example.com");
    existing.setName(null);
    existing.setEmailVerified(false);
    existing.setPasswordHash("hash");
    entityManager.persist(existing);

    dev.auctoritas.auth.entity.oauth.OAuthConnection conn =
        new dev.auctoritas.auth.entity.oauth.OAuthConnection();
    conn.setProject(project);
    conn.setUser(existing);
    conn.setProvider("facebook");
    conn.setProviderUserId("fb-999");
    conn.setEmail("user@example.com");
    entityManager.persist(conn);
    entityManager.flush();

    String state = "state-existing-conn";
    OAuthAuthorizationRequest authRequest = new OAuthAuthorizationRequest();
    authRequest.setProject(project);
    authRequest.setProvider("facebook");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubFacebookOAuthClient.userInfoRef.set(
        new FacebookOAuthClient.FacebookUserInfo("fb-999", "New Name", "Changed@Example.com"));

    callbackService.handleCallback(
        "provider-code", state, "https://gateway.example.com/api/v1/auth/oauth/facebook/callback");

    assertThat(endUserRepository.findAll()).hasSize(1);
    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.getEmailVerified()).isFalse();
              assertThat(user.getName()).isEqualTo("New Name");
            });

    assertThat(
            oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
                project.getId(), "facebook", "fb-999"))
        .isPresent()
        .get()
        .satisfies(
            updated -> {
              assertThat(updated.getEmail()).isEqualTo("changed@example.com");
              assertThat(updated.getUser().getId()).isEqualTo(existing.getId());
            });
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    FacebookOAuthClient facebookOAuthClient() {
      return new StubFacebookOAuthClient();
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

  static class StubFacebookOAuthClient implements FacebookOAuthClient {
    final AtomicReference<FacebookTokenExchangeRequest> exchangeRequestRef = new AtomicReference<>();
    final AtomicReference<FacebookUserInfo> userInfoRef = new AtomicReference<>();

    @Override
    public FacebookTokenResponse exchangeAuthorizationCode(FacebookTokenExchangeRequest request) {
      exchangeRequestRef.set(request);
      return new FacebookTokenResponse("access-token", "bearer", 3600L, null);
    }

    @Override
    public FacebookUserInfo fetchUserInfo(String accessToken) {
      FacebookUserInfo info = userInfoRef.get();
      if (info == null) {
        return new FacebookUserInfo("fb-123", "Test User", "user@example.com");
      }
      return info;
    }
  }
}
