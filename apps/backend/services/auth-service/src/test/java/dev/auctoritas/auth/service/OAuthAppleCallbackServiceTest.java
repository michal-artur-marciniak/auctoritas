package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.adapters.external.oauth.OAuthAppleCallbackService;
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
import dev.auctoritas.auth.service.oauth.OAuthAccountLinkingService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.ports.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import dev.auctoritas.auth.service.oauth.OAuthTokenExchangeRequest;
import dev.auctoritas.auth.service.oauth.OAuthUserInfo;
import dev.auctoritas.auth.shared.enums.OrganizationStatus;
import dev.auctoritas.auth.shared.enums.ProjectStatus;
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
  OAuthAccountLinkingService.class,
  OAuthAppleCallbackService.class,
  OAuthAppleCallbackServiceTest.TestConfig.class
})
class OAuthAppleCallbackServiceTest {

  @org.springframework.beans.factory.annotation.Autowired private EntityManager entityManager;
  @org.springframework.beans.factory.annotation.Autowired private TokenService tokenService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthAppleCallbackService callbackService;

  @org.springframework.beans.factory.annotation.Autowired
  private OAuthAuthorizationRequestRepository oauthAuthorizationRequestRepository;

  @org.springframework.beans.factory.annotation.Autowired private EndUserRepository endUserRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthConnectionRepository oauthConnectionRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthExchangeCodeRepository oauthExchangeCodeRepository;
  @org.springframework.beans.factory.annotation.Autowired private StubAppleOAuthProvider stubAppleOAuthProvider;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = new Organization();
    org.setName("Test Org");
    org.setSlug("test-org-apple-oauth-callback");
    org.setStatus(OrganizationStatus.ACTIVE);
    entityManager.persist(org);
    entityManager.flush();

    ProjectSettings settings = new ProjectSettings();
    Map<String, Object> apple = new HashMap<>();
    apple.put("enabled", true);
    apple.put("serviceId", "apple-service-id");
    apple.put("teamId", "apple-team-id");
    apple.put("keyId", "apple-key-id");

    Map<String, Object> oauthConfig = new HashMap<>();
    oauthConfig.put("apple", apple);
    settings.setOauthConfig(oauthConfig);

    project = new Project();
    project.setOrganization(org);
    project.setName("Test Project");
    project.setSlug("test-project-apple-oauth-callback");
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
    authRequest.setProvider("apple");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubAppleOAuthProvider.userInfoRef.set(new OAuthUserInfo("apple-sub-123", "User@Example.com", true, null));

    String redirectUrl =
        callbackService.handleCallback(
            "provider-code", state, "https://gateway.example.com/api/v1/auth/oauth/apple/callback");

    assertThat(redirectUrl).startsWith("https://example.com/app/callback");
    assertThat(redirectUrl).contains("auctoritas_code=");

    // state is consumed
    assertThat(oauthAuthorizationRequestRepository.findByStateHash(tokenService.hashToken(state))).isEmpty();

    // user created (email is asserted verified for Apple when provided)
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
                project.getId(), "apple", "apple-sub-123"))
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
    conn.setProvider("apple");
    conn.setProviderUserId("apple-sub-999");
    conn.setEmail("user@example.com");
    entityManager.persist(conn);
    entityManager.flush();

    String state = "state-existing-conn";
    OAuthAuthorizationRequest authRequest = new OAuthAuthorizationRequest();
    authRequest.setProject(project);
    authRequest.setProvider("apple");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubAppleOAuthProvider.userInfoRef.set(
        new OAuthUserInfo("apple-sub-999", "Changed@Example.com", false, "New Name"));

    callbackService.handleCallback(
        "provider-code", state, "https://gateway.example.com/api/v1/auth/oauth/apple/callback");

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
                project.getId(), "apple", "apple-sub-999"))
        .isPresent()
        .get()
        .satisfies(updated -> assertThat(updated.getEmail()).isEqualTo("changed@example.com"));
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    OAuthProviderPort appleOAuthProvider() {
      return new StubAppleOAuthProvider();
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

  static class StubAppleOAuthProvider implements OAuthProviderPort {
    final AtomicReference<OAuthTokenExchangeRequest> lastExchangeRequest = new AtomicReference<>();
    final AtomicReference<OAuthUserInfo> userInfoRef = new AtomicReference<>();

    @Override
    public String name() {
      return "apple";
    }

    @Override
    public OAuthAuthorizeDetails getAuthorizeDetails(ProjectSettings settings) {
      return new OAuthAuthorizeDetails("apple-service-id", "https://appleid.apple.com/auth/authorize", "email");
    }

    @Override
    public String buildAuthorizeUrl(OAuthAuthorizeDetails details, OAuthAuthorizeUrlRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public OAuthUserInfo exchangeAuthorizationCode(ProjectSettings settings, OAuthTokenExchangeRequest request) {
      lastExchangeRequest.set(request);
      OAuthUserInfo info = userInfoRef.get();
      if (info != null) {
        return info;
      }
      return new OAuthUserInfo("apple-sub-123", "user@example.com", true, null);
    }
  }
}
