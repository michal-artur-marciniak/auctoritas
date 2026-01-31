package dev.auctoritas.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.service.ApiKeyService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthAppleAuthorizationService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthAppleCallbackService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthFacebookAuthorizationService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthFacebookCallbackService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthGitHubAuthorizationService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthGitHubCallbackService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthGoogleAuthorizationService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthGoogleCallbackService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthMicrosoftAuthorizationService;
import dev.auctoritas.auth.adapters.external.oauth.OAuthMicrosoftCallbackService;
import dev.auctoritas.auth.service.TokenService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.ports.oauth.OAuthProviderPort;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import dev.auctoritas.auth.domain.project.Slug;
import dev.auctoritas.auth.shared.persistence.BaseEntity;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class PublicOAuthControllerTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private FilterChainProxy springSecurityFilterChain;

  private MockMvc mockMvc;

  @MockitoBean private ApiKeyService apiKeyService;
  @MockitoBean private TokenService tokenService;
  @MockitoBean private ProjectRepository projectRepository;
  @MockitoBean private OAuthProviderRegistry oauthProviderRegistry;

  @MockitoBean private OAuthGoogleAuthorizationService oauthGoogleAuthorizationService;
  @MockitoBean private OAuthGoogleCallbackService oauthGoogleCallbackService;
  @MockitoBean private OAuthGitHubAuthorizationService oauthGitHubAuthorizationService;
  @MockitoBean private OAuthGitHubCallbackService oauthGitHubCallbackService;
  @MockitoBean private OAuthMicrosoftAuthorizationService oauthMicrosoftAuthorizationService;
  @MockitoBean private OAuthMicrosoftCallbackService oauthMicrosoftCallbackService;
  @MockitoBean private OAuthFacebookAuthorizationService oauthFacebookAuthorizationService;
  @MockitoBean private OAuthFacebookCallbackService oauthFacebookCallbackService;
  @MockitoBean private OAuthAppleAuthorizationService oauthAppleAuthorizationService;
  @MockitoBean private OAuthAppleCallbackService oauthAppleCallbackService;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(springSecurityFilterChain)
            .build();
  }

  @Test
  @DisplayName("authorize: missing X-API-Key returns 401 and controller runs")
  void authorizeMissingApiKeyReturns401() throws Exception {
    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    when(oauthProviderRegistry.require("google")).thenReturn(provider);

    when(apiKeyService.validateActiveKey(null))
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid"));

    mockMvc
        .perform(get("/api/v1/auth/oauth/google/authorize").secure(true))
        .andExpect(status().isUnauthorized());

    verify(apiKeyService).validateActiveKey(null);
    verify(oauthGoogleAuthorizationService, never())
        .createAuthorizationRequest(any(), any(), any(), any());
  }

  @Test
  @DisplayName("authorize: unknown provider returns 400 and does not validate API key")
  void authorizeUnknownProviderReturns400() throws Exception {
    when(oauthProviderRegistry.require("unknown"))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid"));

    mockMvc
        .perform(get("/api/v1/auth/oauth/unknown/authorize").secure(true))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(apiKeyService);
  }

  @Test
  @DisplayName("authorize alias: missing X-API-Key returns 401 and controller runs")
  void authorizeAliasMissingApiKeyReturns401() throws Exception {
    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    when(oauthProviderRegistry.require("google")).thenReturn(provider);

    when(apiKeyService.validateActiveKey(null))
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid"));

    mockMvc
        .perform(get("/api/v1/auth/oauth/google").secure(true))
        .andExpect(status().isUnauthorized());

    verify(apiKeyService).validateActiveKey(null);
    verify(oauthGoogleAuthorizationService, never())
        .createAuthorizationRequest(any(), any(), any(), any());
  }

  @Test
  @DisplayName("authorize: valid X-API-Key returns 302 and includes state + PKCE params")
  void authorizeValidApiKeyReturnsRedirectWithPkce() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    Organization organization = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(organization, "Test", Slug.of("test"));
    setEntityId(project, projectId);
    ProjectSettings settings = project.getSettings();

    ApiKey key = ApiKey.create(project, "Test Key", "pk_test_", "hash");

    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthAuthorizeDetails details =
        new OAuthAuthorizeDetails("client-id", "https://provider.example.com/authorize", "openid");

    when(oauthProviderRegistry.require("google")).thenReturn(provider);
    when(apiKeyService.validateActiveKey("pk_test_123")).thenReturn(key);
    when(tokenService.generateOAuthState()).thenReturn("state-123");
    when(tokenService.generateOAuthCodeVerifier()).thenReturn("verifier-123");
    when(tokenService.hashToken("verifier-123")).thenReturn("challenge-123");
    when(projectRepository.findByIdWithSettings(projectId)).thenReturn(Optional.of(project));
    when(provider.getAuthorizeDetails(settings)).thenReturn(details);

    AtomicReference<String> authorizeUrlRef = new AtomicReference<>();
    when(provider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenAnswer(
            invocation -> {
              OAuthAuthorizeUrlRequest request = invocation.getArgument(1);
              String url =
                  "https://provider.example.com/authorize"
                      + "?state="
                      + request.state()
                      + "&code_challenge="
                      + request.codeChallenge()
                      + "&code_challenge_method="
                      + request.codeChallengeMethod();
              authorizeUrlRef.set(url);
              return url;
            });

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/auth/oauth/google/authorize")
                    .secure(true)
                    .header("X-API-Key", "pk_test_123")
                    .queryParam("redirect_uri", "https://example.com/app/callback"))
            .andExpect(status().isFound())
            .andReturn();

    String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
    assertThat(location).isEqualTo(authorizeUrlRef.get());
    assertThat(location).contains("state=state-123");
    assertThat(location).contains("code_challenge=challenge-123");
    assertThat(location).contains("code_challenge_method=S256");

    ArgumentCaptor<OAuthAuthorizeUrlRequest> requestCaptor =
        ArgumentCaptor.forClass(OAuthAuthorizeUrlRequest.class);
    verify(provider).buildAuthorizeUrl(eq(details), requestCaptor.capture());
    OAuthAuthorizeUrlRequest request = requestCaptor.getValue();
    assertThat(request.state()).isEqualTo("state-123");
    assertThat(request.codeChallenge()).isEqualTo("challenge-123");
    assertThat(request.codeChallengeMethod()).isEqualTo("S256");
    URI callbackUri = new URI(request.callbackUri());
    assertThat(callbackUri.getScheme()).isIn("http", "https");
    assertThat(callbackUri.getPath()).endsWith("/api/v1/auth/oauth/google/callback");

    verify(oauthGoogleAuthorizationService)
        .createAuthorizationRequest(
            eq(projectId),
            eq("https://example.com/app/callback"),
            eq("state-123"),
            eq("verifier-123"));
  }

  @Test
  @DisplayName("authorize: provider normalization lowercases path variable")
  void authorizeNormalizesProvider() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    Organization organization = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(organization, "Test", Slug.of("test"));
    setEntityId(project, projectId);
    ProjectSettings settings = project.getSettings();

    ApiKey key = ApiKey.create(project, "Test Key", "pk_test_", "hash");

    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthAuthorizeDetails details =
        new OAuthAuthorizeDetails("client-id", "https://provider.example.com/authorize", "openid");

    when(oauthProviderRegistry.require("google")).thenReturn(provider);
    when(apiKeyService.validateActiveKey("pk_test_123")).thenReturn(key);
    when(tokenService.generateOAuthState()).thenReturn("state-123");
    when(tokenService.generateOAuthCodeVerifier()).thenReturn("verifier-123");
    when(tokenService.hashToken("verifier-123")).thenReturn("challenge-123");
    when(projectRepository.findByIdWithSettings(projectId)).thenReturn(Optional.of(project));
    when(provider.getAuthorizeDetails(settings)).thenReturn(details);
    when(provider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenReturn("https://provider.example.com/authorize?state=state-123");

    mockMvc
        .perform(get("/api/v1/auth/oauth/GOOGLE/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isFound());

    verify(oauthProviderRegistry).require("google");
  }

  @Test
  @DisplayName("authorize: project not found returns 404")
  void authorizeProjectNotFoundReturns404() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    Organization organization = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(organization, "Test", Slug.of("test"));
    setEntityId(project, projectId);
    ApiKey key = ApiKey.create(project, "Test Key", "pk_test_", "hash");

    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    when(oauthProviderRegistry.require("google")).thenReturn(provider);
    when(apiKeyService.validateActiveKey("pk_test_123")).thenReturn(key);
    when(tokenService.generateOAuthState()).thenReturn("state-123");
    when(tokenService.generateOAuthCodeVerifier()).thenReturn("verifier-123");
    when(projectRepository.findByIdWithSettings(projectId)).thenReturn(Optional.empty());

    mockMvc
        .perform(get("/api/v1/auth/oauth/google/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isNotFound());

    verify(oauthGoogleAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
  }

  @Test
  @DisplayName("authorize: missing project settings returns 400")
  void authorizeProjectSettingsMissingReturns400() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    Organization organization = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(organization, "Test", Slug.of("test"));
    setEntityId(project, projectId);
    clearProjectSettings(project);
    ApiKey key = ApiKey.create(project, "Test Key", "pk_test_", "hash");

    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    when(oauthProviderRegistry.require("google")).thenReturn(provider);
    when(apiKeyService.validateActiveKey("pk_test_123")).thenReturn(key);
    when(tokenService.generateOAuthState()).thenReturn("state-123");
    when(tokenService.generateOAuthCodeVerifier()).thenReturn("verifier-123");
    when(projectRepository.findByIdWithSettings(projectId)).thenReturn(Optional.of(project));

    mockMvc
        .perform(get("/api/v1/auth/oauth/google/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isBadRequest());

    verify(oauthGoogleAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
  }

  @Test
  @DisplayName("authorize alias: valid X-API-Key returns 302 and includes state + PKCE params")
  void authorizeAliasValidApiKeyReturnsRedirectWithPkce() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    Organization organization = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(organization, "Test", Slug.of("test"));
    setEntityId(project, projectId);
    ProjectSettings settings = project.getSettings();

    ApiKey key = ApiKey.create(project, "Test Key", "pk_test_", "hash");

    OAuthProviderPort provider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthAuthorizeDetails details =
        new OAuthAuthorizeDetails("client-id", "https://provider.example.com/authorize", "openid");

    when(oauthProviderRegistry.require("google")).thenReturn(provider);
    when(apiKeyService.validateActiveKey("pk_test_123")).thenReturn(key);
    when(tokenService.generateOAuthState()).thenReturn("state-123");
    when(tokenService.generateOAuthCodeVerifier()).thenReturn("verifier-123");
    when(tokenService.hashToken("verifier-123")).thenReturn("challenge-123");
    when(projectRepository.findByIdWithSettings(projectId)).thenReturn(Optional.of(project));
    when(provider.getAuthorizeDetails(settings)).thenReturn(details);

    AtomicReference<String> authorizeUrlRef = new AtomicReference<>();
    when(provider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenAnswer(
            invocation -> {
              OAuthAuthorizeUrlRequest request = invocation.getArgument(1);
              String url =
                  "https://provider.example.com/authorize"
                      + "?state="
                      + request.state()
                      + "&code_challenge="
                      + request.codeChallenge()
                      + "&code_challenge_method="
                      + request.codeChallengeMethod();
              authorizeUrlRef.set(url);
              return url;
            });

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/auth/oauth/google")
                    .secure(true)
                    .header("X-API-Key", "pk_test_123")
                    .queryParam("redirect_uri", "https://example.com/app/callback"))
            .andExpect(status().isFound())
            .andReturn();

    String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
    assertThat(location).isEqualTo(authorizeUrlRef.get());
    assertThat(location).contains("state=state-123");
    assertThat(location).contains("code_challenge=challenge-123");
    assertThat(location).contains("code_challenge_method=S256");

    ArgumentCaptor<OAuthAuthorizeUrlRequest> requestCaptor =
        ArgumentCaptor.forClass(OAuthAuthorizeUrlRequest.class);
    verify(provider).buildAuthorizeUrl(eq(details), requestCaptor.capture());
    OAuthAuthorizeUrlRequest request = requestCaptor.getValue();
    assertThat(request.state()).isEqualTo("state-123");
    assertThat(request.codeChallenge()).isEqualTo("challenge-123");
    assertThat(request.codeChallengeMethod()).isEqualTo("S256");
    URI callbackUri = new URI(request.callbackUri());
    assertThat(callbackUri.getScheme()).isIn("http", "https");
    assertThat(callbackUri.getPath()).endsWith("/api/v1/auth/oauth/google/callback");

    verify(oauthGoogleAuthorizationService)
        .createAuthorizationRequest(
            eq(projectId),
            eq("https://example.com/app/callback"),
            eq("state-123"),
            eq("verifier-123"));
  }

  @Test
  @DisplayName("authorize: delegates to provider authorization service (google/github/microsoft/facebook/apple)")
  void authorizeDelegatesToProviderAuthorizationService() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    Organization organization = Organization.create("Test", Slug.of("test"));
    Project project = Project.create(organization, "Test", Slug.of("test"));
    setEntityId(project, projectId);
    ProjectSettings settings = project.getSettings();

    ApiKey key = ApiKey.create(project, "Test Key", "pk_test_", "hash");

    OAuthAuthorizeDetails details =
        new OAuthAuthorizeDetails("client-id", "https://provider.example.com/authorize", "openid");

    when(apiKeyService.validateActiveKey("pk_test_123")).thenReturn(key);
    when(tokenService.generateOAuthState()).thenReturn("state-123");
    when(tokenService.generateOAuthCodeVerifier()).thenReturn("verifier-123");
    when(tokenService.hashToken("verifier-123")).thenReturn("challenge-123");
    when(projectRepository.findByIdWithSettings(projectId)).thenReturn(Optional.of(project));

    OAuthProviderPort googleProvider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthProviderPort githubProvider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthProviderPort microsoftProvider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthProviderPort facebookProvider = org.mockito.Mockito.mock(OAuthProviderPort.class);
    OAuthProviderPort appleProvider = org.mockito.Mockito.mock(OAuthProviderPort.class);

    when(oauthProviderRegistry.require("google")).thenReturn(googleProvider);
    when(oauthProviderRegistry.require("github")).thenReturn(githubProvider);
    when(oauthProviderRegistry.require("microsoft")).thenReturn(microsoftProvider);
    when(oauthProviderRegistry.require("facebook")).thenReturn(facebookProvider);
    when(oauthProviderRegistry.require("apple")).thenReturn(appleProvider);

    when(googleProvider.getAuthorizeDetails(settings)).thenReturn(details);
    when(githubProvider.getAuthorizeDetails(settings)).thenReturn(details);
    when(microsoftProvider.getAuthorizeDetails(settings)).thenReturn(details);
    when(facebookProvider.getAuthorizeDetails(settings)).thenReturn(details);
    when(appleProvider.getAuthorizeDetails(settings)).thenReturn(details);

    when(googleProvider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenReturn("https://provider.example.com/authorize?provider=google");
    when(githubProvider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenReturn("https://provider.example.com/authorize?provider=github");
    when(microsoftProvider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenReturn("https://provider.example.com/authorize?provider=microsoft");
    when(facebookProvider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenReturn("https://provider.example.com/authorize?provider=facebook");
    when(appleProvider.buildAuthorizeUrl(eq(details), any(OAuthAuthorizeUrlRequest.class)))
        .thenReturn("https://provider.example.com/authorize?provider=apple");

    mockMvc
        .perform(get("/api/v1/auth/oauth/google/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isFound());
    mockMvc
        .perform(get("/api/v1/auth/oauth/github/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isFound());
    mockMvc
        .perform(
            get("/api/v1/auth/oauth/microsoft/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isFound());
    mockMvc
        .perform(get("/api/v1/auth/oauth/facebook/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isFound());
    mockMvc
        .perform(get("/api/v1/auth/oauth/apple/authorize").secure(true).header("X-API-Key", "pk_test_123"))
        .andExpect(status().isFound());

    verify(oauthGoogleAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
    verify(oauthGitHubAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
    verify(oauthMicrosoftAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
    verify(oauthFacebookAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
    verify(oauthAppleAuthorizationService)
        .createAuthorizationRequest(eq(projectId), isNull(), eq("state-123"), eq("verifier-123"));
  }

  @Test
  @DisplayName("callback: missing code returns 400")
  void callbackMissingCodeReturns400() throws Exception {
    when(oauthProviderRegistry.require("google")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));
    when(oauthGoogleCallbackService.handleCallback(
            isNull(), eq("state-123"), any(String.class)))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_code_missing"));

    mockMvc
        .perform(
            get("/api/v1/auth/oauth/google/callback")
                .secure(true)
                .queryParam("state", "state-123"))
        .andExpect(status().isBadRequest());

    ArgumentCaptor<String> callbackUriCaptor = ArgumentCaptor.forClass(String.class);
    verify(oauthGoogleCallbackService)
        .handleCallback(isNull(), eq("state-123"), callbackUriCaptor.capture());
    assertThat(callbackUriCaptor.getValue()).endsWith("/api/v1/auth/oauth/google/callback");
  }

  @Test
  @DisplayName("callback: unknown provider returns 400 and does not invoke callback handler")
  void callbackUnknownProviderReturns400() throws Exception {
    when(oauthProviderRegistry.require("unknown"))
        .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "oauth_provider_invalid"));

    mockMvc
        .perform(get("/api/v1/auth/oauth/unknown/callback").secure(true).queryParam("code", "c"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(oauthGoogleCallbackService);
    verifyNoInteractions(oauthGitHubCallbackService);
    verifyNoInteractions(oauthMicrosoftCallbackService);
    verifyNoInteractions(oauthFacebookCallbackService);
    verifyNoInteractions(oauthAppleCallbackService);
  }

  @Test
  @DisplayName("callback: delegates to provider callback service (google/github/microsoft/facebook/apple)")
  void callbackDelegatesToProviderCallbackService() throws Exception {
    when(oauthProviderRegistry.require("google")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));
    when(oauthProviderRegistry.require("github")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));
    when(oauthProviderRegistry.require("microsoft")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));
    when(oauthProviderRegistry.require("facebook")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));
    when(oauthProviderRegistry.require("apple")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));

    when(oauthGoogleCallbackService.handleCallback(eq("provider-code"), eq("state"), any(String.class)))
        .thenReturn("https://example.com/google");
    when(oauthGitHubCallbackService.handleCallback(eq("provider-code"), eq("state"), any(String.class)))
        .thenReturn("https://example.com/github");
    when(oauthMicrosoftCallbackService.handleCallback(eq("provider-code"), eq("state"), any(String.class)))
        .thenReturn("https://example.com/microsoft");
    when(oauthFacebookCallbackService.handleCallback(eq("provider-code"), eq("state"), any(String.class)))
        .thenReturn("https://example.com/facebook");
    when(oauthAppleCallbackService.handleCallback(eq("provider-code"), eq("state"), any(String.class)))
        .thenReturn("https://example.com/apple");

    assertThat(
            mockMvc
                .perform(
                    get("/api/v1/auth/oauth/google/callback")
                        .secure(true)
                        .queryParam("code", "provider-code")
                        .queryParam("state", "state"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION))
        .isEqualTo("https://example.com/google");

    assertThat(
            mockMvc
                .perform(
                    get("/api/v1/auth/oauth/github/callback")
                        .secure(true)
                        .queryParam("code", "provider-code")
                        .queryParam("state", "state"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION))
        .isEqualTo("https://example.com/github");

    assertThat(
            mockMvc
                .perform(
                    get("/api/v1/auth/oauth/microsoft/callback")
                        .secure(true)
                        .queryParam("code", "provider-code")
                        .queryParam("state", "state"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION))
        .isEqualTo("https://example.com/microsoft");

    assertThat(
            mockMvc
                .perform(
                    get("/api/v1/auth/oauth/facebook/callback")
                        .secure(true)
                        .queryParam("code", "provider-code")
                        .queryParam("state", "state"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION))
        .isEqualTo("https://example.com/facebook");

    assertThat(
            mockMvc
                .perform(
                    get("/api/v1/auth/oauth/apple/callback")
                        .secure(true)
                        .queryParam("code", "provider-code")
                        .queryParam("state", "state"))
                .andExpect(status().isFound())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.LOCATION))
        .isEqualTo("https://example.com/apple");

    verify(oauthGoogleCallbackService).handleCallback(eq("provider-code"), eq("state"), any(String.class));
    verify(oauthGitHubCallbackService).handleCallback(eq("provider-code"), eq("state"), any(String.class));
    verify(oauthMicrosoftCallbackService).handleCallback(eq("provider-code"), eq("state"), any(String.class));
    verify(oauthFacebookCallbackService).handleCallback(eq("provider-code"), eq("state"), any(String.class));
    verify(oauthAppleCallbackService).handleCallback(eq("provider-code"), eq("state"), any(String.class));
  }

  @Test
  @DisplayName("callback: success returns 302 to app redirect URL")
  void callbackSuccessReturnsRedirect() throws Exception {
    when(oauthProviderRegistry.require("google")).thenReturn(org.mockito.Mockito.mock(OAuthProviderPort.class));
    when(oauthGoogleCallbackService.handleCallback(
            eq("provider-code"), eq("state-123"), any(String.class)))
        .thenReturn("https://example.com/app/callback?auctoritas_code=abc");

    MvcResult result =
        mockMvc
            .perform(
                get("/api/v1/auth/oauth/google/callback")
                    .secure(true)
                    .queryParam("code", "provider-code")
                    .queryParam("state", "state-123"))
            .andExpect(status().isFound())
            .andReturn();

    assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
        .isEqualTo("https://example.com/app/callback?auctoritas_code=abc");
  }

  private void setEntityId(BaseEntity entity, UUID id) {
    try {
      var setId = BaseEntity.class.getDeclaredMethod("setId", UUID.class);
      setId.setAccessible(true);
      setId.invoke(entity, id);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Failed to set entity id", ex);
    }
  }

  private void clearProjectSettings(Project project) {
    try {
      var field = Project.class.getDeclaredField("settings");
      field.setAccessible(true);
      field.set(project, null);
    } catch (ReflectiveOperationException ex) {
      throw new IllegalStateException("Failed to clear project settings", ex);
    }
  }
}
