package dev.auctoritas.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.auctoritas.auth.adapters.external.oauth.GitHubOAuthClient;
import dev.auctoritas.auth.adapters.external.oauth.OAuthGitHubCallbackService;
import dev.auctoritas.auth.adapters.infra.jpa.EndUserJpaRepositoryAdapter;
import dev.auctoritas.auth.adapters.persistence.OAuthAuthorizationRequestJpaRepositoryAdapter;
import dev.auctoritas.auth.adapters.persistence.OAuthConnectionJpaRepositoryAdapter;
import dev.auctoritas.auth.adapters.persistence.OAuthExchangeCodeJpaRepositoryAdapter;
import dev.auctoritas.auth.config.JpaConfig;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.oauth.OAuthAuthorizationRequest;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthAuthorizationRequestRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthConnectionRepositoryPort;
import dev.auctoritas.auth.ports.oauth.OAuthExchangeCodeRepositoryPort;
import dev.auctoritas.auth.adapters.external.oauth.GitHubOAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthAccountLinkingService;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Password;
import dev.auctoritas.auth.domain.valueobject.Slug;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
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
@Import({
  JpaConfig.class,
  TokenService.class,
  OAuthProviderRegistry.class,
  GitHubOAuthProvider.class,
  OAuthAccountLinkingService.class,
  OAuthGitHubCallbackService.class,
  EndUserJpaRepositoryAdapter.class,
  OAuthAuthorizationRequestJpaRepositoryAdapter.class,
  OAuthConnectionJpaRepositoryAdapter.class,
  OAuthExchangeCodeJpaRepositoryAdapter.class,
  OAuthGitHubCallbackServiceTest.TestConfig.class
})
class OAuthGitHubCallbackServiceTest {

  @org.springframework.beans.factory.annotation.Autowired private EntityManager entityManager;
  @org.springframework.beans.factory.annotation.Autowired private TokenService tokenService;
  @org.springframework.beans.factory.annotation.Autowired private OAuthGitHubCallbackService callbackService;
  @org.springframework.beans.factory.annotation.Autowired
  private OAuthAuthorizationRequestRepositoryPort oauthAuthorizationRequestRepository;
  @org.springframework.beans.factory.annotation.Autowired private EndUserRepositoryPort endUserRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthConnectionRepositoryPort oauthConnectionRepository;
  @org.springframework.beans.factory.annotation.Autowired private OAuthExchangeCodeRepositoryPort oauthExchangeCodeRepository;
  @org.springframework.beans.factory.annotation.Autowired private StubGitHubOAuthClient stubGitHubOAuthClient;

  private Project project;

  @BeforeEach
  void setUp() {
    Organization org = Organization.create("Test Org", Slug.of("test-org-github-oauth-callback"));
    entityManager.persist(org);
    entityManager.flush();

    project = Project.create(org, "Test Project", Slug.of("test-project-github-oauth-callback"));
    ProjectSettings settings = project.getSettings();

    Map<String, Object> github = new HashMap<>();
    github.put("enabled", true);
    github.put("clientId", "github-client-id");

    Map<String, Object> oauthConfig = new HashMap<>();
    oauthConfig.put("github", github);
    settings.updateOauthConfig(oauthConfig);
    settings.setOauthGithubClientSecretEnc("github-client-secret");

    entityManager.persist(project);
    entityManager.flush();
  }

  @Test
  @DisplayName("Should exchange provider code, create/link user, and issue one-time exchange code")
  void shouldHandleCallbackAndIssueExchangeCode() {
    String state = "state-123";
    OAuthAuthorizationRequest authRequest = new OAuthAuthorizationRequest();
    authRequest.setProject(project);
    authRequest.setProvider("github");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubGitHubOAuthClient.userRef.set(new GitHubOAuthClient.GitHubUser(123L, "octo", "Test User", null));
    stubGitHubOAuthClient.emailsRef.set(
        List.of(new GitHubOAuthClient.GitHubUserEmail("User@Example.com", true, true, null)));

    String redirectUrl =
        callbackService.handleCallback(
            "provider-code",
            state,
            "https://gateway.example.com/api/v1/auth/oauth/github/callback");

    assertThat(redirectUrl).startsWith("https://example.com/app/callback");
    assertThat(redirectUrl).contains("auctoritas_code=");

    // state is consumed
    assertThat(oauthAuthorizationRequestRepository.findByStateHash(tokenService.hashToken(state))).isEmpty();

    // user created and verified
    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.isEmailVerified()).isTrue();
              assertThat(user.getPasswordHash()).isNotBlank();
            });

    // oauth connection created
    assertThat(
            oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
                project.getId(), "github", "123"))
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
  @DisplayName("Should mark existing end-user as emailVerified when signing in with GitHub")
  void shouldMarkExistingUserVerified() {
    EndUser existing = EndUser.create(project, Email.of("user@example.com"), Password.fromHash("hash"), null);
    entityManager.persist(existing);
    entityManager.flush();

    String state = "state-existing-user";
    OAuthAuthorizationRequest authRequest = new OAuthAuthorizationRequest();
    authRequest.setProject(project);
    authRequest.setProvider("github");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubGitHubOAuthClient.userRef.set(new GitHubOAuthClient.GitHubUser(456L, "octo", "Existing Name", null));
    stubGitHubOAuthClient.emailsRef.set(
        List.of(new GitHubOAuthClient.GitHubUserEmail("user@example.com", true, true, null)));

    callbackService.handleCallback(
        "provider-code",
        state,
        "https://gateway.example.com/api/v1/auth/oauth/github/callback");

    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.isEmailVerified()).isTrue();
              assertThat(user.getName()).isEqualTo("Existing Name");
            });
  }

  @Test
  @DisplayName("Should use existing OAuth connection when provider_user_id matches")
  void shouldUseExistingConnectionByProviderUserId() {
    EndUser existing = EndUser.create(project, Email.of("user@example.com"), Password.fromHash("hash"), null);
    entityManager.persist(existing);

    dev.auctoritas.auth.domain.model.oauth.OAuthConnection conn = new dev.auctoritas.auth.domain.model.oauth.OAuthConnection();
    conn.setProject(project);
    conn.setUser(existing);
    conn.setProvider("github");
    conn.setProviderUserId("789");
    conn.setEmail("user@example.com");
    entityManager.persist(conn);
    entityManager.flush();

    String state = "state-existing-conn";
    OAuthAuthorizationRequest authRequest = new OAuthAuthorizationRequest();
    authRequest.setProject(project);
    authRequest.setProvider("github");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubGitHubOAuthClient.userRef.set(new GitHubOAuthClient.GitHubUser(789L, "octo", "New Name", null));
    stubGitHubOAuthClient.emailsRef.set(
        List.of(new GitHubOAuthClient.GitHubUserEmail("Changed@Example.com", true, false, null)));

    callbackService.handleCallback(
        "provider-code",
        state,
        "https://gateway.example.com/api/v1/auth/oauth/github/callback");

    assertThat(entityManager.createQuery("SELECT u FROM EndUser u", EndUser.class).getResultList()).hasSize(1);
    assertThat(endUserRepository.findByEmailAndProjectId("user@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.isEmailVerified()).isFalse();
              assertThat(user.getName()).isEqualTo("New Name");
            });

    assertThat(
            oauthConnectionRepository.findByProjectIdAndProviderAndProviderUserId(
                project.getId(), "github", "789"))
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
    authRequest.setProvider("github");
    authRequest.setStateHash(tokenService.hashToken(state));
    authRequest.setCodeVerifier("pkce-verifier");
    authRequest.setAppRedirectUri("https://example.com/app/callback");
    authRequest.setExpiresAt(Instant.now().plusSeconds(600));
    entityManager.persist(authRequest);
    entityManager.flush();

    stubGitHubOAuthClient.userRef.set(new GitHubOAuthClient.GitHubUser(321L, "octo", "Unverified User", null));
    stubGitHubOAuthClient.emailsRef.set(
        List.of(new GitHubOAuthClient.GitHubUserEmail("user2@example.com", true, false, null)));

    callbackService.handleCallback(
        "provider-code",
        state,
        "https://gateway.example.com/api/v1/auth/oauth/github/callback");

    assertThat(endUserRepository.findByEmailAndProjectId("user2@example.com", project.getId()))
        .isPresent()
        .get()
        .satisfies(
            user -> {
              assertThat(user.isEmailVerified()).isFalse();
              assertThat(user.getName()).isEqualTo("Unverified User");
            });
  }

  @TestConfiguration
  static class TestConfig {
    @Bean
    @Primary
    GitHubOAuthClient gitHubOAuthClient() {
      return new StubGitHubOAuthClient();
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

  static class StubGitHubOAuthClient implements GitHubOAuthClient {
    final AtomicReference<GitHubTokenExchangeRequest> exchangeRequestRef = new AtomicReference<>();
    final AtomicReference<GitHubUser> userRef = new AtomicReference<>();
    final AtomicReference<List<GitHubUserEmail>> emailsRef = new AtomicReference<>();

    @Override
    public GitHubTokenResponse exchangeAuthorizationCode(GitHubTokenExchangeRequest request) {
      exchangeRequestRef.set(request);
      return new GitHubTokenResponse("access-token", "bearer", "read:user user:email");
    }

    @Override
    public GitHubUser fetchUser(String accessToken) {
      GitHubUser user = userRef.get();
      if (user == null) {
        return new GitHubUser(123L, "octo", "Test User", null);
      }
      return user;
    }

    @Override
    public List<GitHubUserEmail> fetchUserEmails(String accessToken) {
      List<GitHubUserEmail> emails = emailsRef.get();
      if (emails == null) {
        return List.of(new GitHubUserEmail("user@example.com", true, true, null));
      }
      return emails;
    }
  }
}
