package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.adapters.external.oauth.GoogleOAuthClient;
import dev.auctoritas.auth.adapters.external.oauth.OAuthGoogleCallbackService;
import dev.auctoritas.auth.adapters.infra.jpa.EndUserJpaRepositoryAdapter;
import dev.auctoritas.auth.adapters.persistence.OAuthAuthorizationRequestJpaRepositoryAdapter;
import dev.auctoritas.auth.adapters.persistence.OAuthConnectionJpaRepositoryAdapter;
import dev.auctoritas.auth.adapters.persistence.OAuthExchangeCodeJpaRepositoryAdapter;
import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.entity.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthAuthorizationRequestRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthConnectionRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthExchangeCodeRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationStatus;
import dev.auctoritas.auth.domain.project.ProjectStatus;
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
import dev.auctoritas.auth.adapters.external.oauth.GoogleOAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthAccountLinkingService;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;

@DataJpaTest
@ActiveProfiles("test")
@Import({
  JpaConfig.class,
  TokenService.class,
  OAuthProviderRegistry.class,
  GoogleOAuthProvider.class,
  OAuthAccountLinkingService.class,
  OAuthGoogleCallbackService.class,
  EndUserJpaRepositoryAdapter.class,
  OAuthConnectionJpaRepositoryAdapter.class,
  OAuthAuthorizationRequestJpaRepositoryAdapter.class,
  OAuthExchangeCodeJpaRepositoryAdapter.class,
  OAuthGoogleCallbackServiceTest.TestConfig.class
})
class OAuthGoogleCallbackServiceTest {

  @org.springframework.beans.factory.annotation.Autowired private EntityManager entityManager;
  @org.springframework.beans.factory.annotation.Autowired private TokenService tokenService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthGoogleCallbackService callbackService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthAuthorizationRequestRepositoryPort oauthAuthorizationRequestRepository;
  @org.springframework.beans.factory.annotation.Autowired private EndUserRepositoryPort endUserRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthConnectionRepositoryPort oauthConnectionRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository;
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

  @Test
  @DisplayName("Should mark existing end-user as emailVerified when signing in with Google")
  void shouldMarkExistingUserVerified() {
    EndUser existing = new EndUser();
    existing.setProject(project);
    existing.setEmail("user@example.com");
    existing.setName(null);
    existing.setEmailVerified(false);
    existing.setPasswordHash("hash");
    entityManager.persist(existing);
    entityManager.flush();

    String state = "state-existing-user";
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
        new GoogleOAuthClient.GoogleUserInfo("google-sub-existing", "user@example.com", true, "Existing Name"));

    callbackService.handleCallback(
        "provider-code",
        state,
        "https://gateway.example.com/api/v1/auth/oauth/google/callback");

    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.getEmailVerified()).isTrue();
              assertThat(user.getName()).isEqualTo("Existing Name");
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
    conn.setProvider("google");
    conn.setProviderUserId("google-sub-conn");
    conn.setEmail("user@example.com");
    entityManager.persist(conn);
    entityManager.flush();

    String state = "state-existing-conn";
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
        new GoogleOAuthClient.GoogleUserInfo(
            "google-sub-conn", "Changed@Example.com", false, "New Name"));

    callbackService.handleCallback(
        "provider-code",
        state,
        "https://gateway.example.com/api/v1/auth/oauth/google/callback");

    assertThat(entityManager.createQuery("SELECT u FROM EndUser u", EndUser.class).getResultList()).hasSize(1);
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
                project.getId(), "google", "google-sub-conn"))
        .isPresent()
        .get()
        .satisfies(
            updated -> {
              assertThat(updated.getEmail()).isEqualTo("changed@example.com");
              assertThat(updated.getUser().getId()).isEqualTo(existing.getId());
            });
  }

  @Test
  @DisplayName("Should create user with emailVerified=false when provider email is not verified")
  void shouldCreateUnverifiedUserWhenProviderEmailNotVerified() {
    String state = "state-unverified";
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
        new GoogleOAuthClient.GoogleUserInfo(
            "google-sub-2", "user2@example.com", false, "Unverified User"));

    callbackService.handleCallback(
        "provider-code",
        state,
        "https://gateway.example.com/api/v1/auth/oauth/google/callback");

    assertThat(endUserRepository.findByEmailAndProjectId("user2@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.getEmailVerified()).isFalse();
              assertThat(user.getName()).isEqualTo("Unverified User");
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
