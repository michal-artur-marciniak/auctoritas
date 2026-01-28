package dev.auctoritas.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.repository.ProjectRepository;
import dev.auctoritas.auth.service.ApiKeyService;
import dev.auctoritas.auth.service.OAuthAppleAuthorizationService;
import dev.auctoritas.auth.service.OAuthAppleCallbackService;
import dev.auctoritas.auth.service.OAuthFacebookAuthorizationService;
import dev.auctoritas.auth.service.OAuthFacebookCallbackService;
import dev.auctoritas.auth.service.OAuthGitHubAuthorizationService;
import dev.auctoritas.auth.service.OAuthGitHubCallbackService;
import dev.auctoritas.auth.service.OAuthGoogleAuthorizationService;
import dev.auctoritas.auth.service.OAuthGoogleCallbackService;
import dev.auctoritas.auth.service.OAuthMicrosoftAuthorizationService;
import dev.auctoritas.auth.service.OAuthMicrosoftCallbackService;
import dev.auctoritas.auth.service.TokenService;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeDetails;
import dev.auctoritas.auth.service.oauth.OAuthAuthorizeUrlRequest;
import dev.auctoritas.auth.service.oauth.OAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
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
    OAuthProvider provider = org.mockito.Mockito.mock(OAuthProvider.class);
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
  @DisplayName("authorize alias: missing X-API-Key returns 401 and controller runs")
  void authorizeAliasMissingApiKeyReturns401() throws Exception {
    OAuthProvider provider = org.mockito.Mockito.mock(OAuthProvider.class);
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

    ProjectSettings settings = new ProjectSettings();
    Project project = new Project();
    project.setId(projectId);
    project.setSettings(settings);
    settings.setProject(project);

    ApiKey key = new ApiKey();
    key.setProject(project);

    OAuthProvider provider = org.mockito.Mockito.mock(OAuthProvider.class);
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
  @DisplayName("authorize alias: valid X-API-Key returns 302 and includes state + PKCE params")
  void authorizeAliasValidApiKeyReturnsRedirectWithPkce() throws Exception {
    UUID projectId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    ProjectSettings settings = new ProjectSettings();
    Project project = new Project();
    project.setId(projectId);
    project.setSettings(settings);
    settings.setProject(project);

    ApiKey key = new ApiKey();
    key.setProject(project);

    OAuthProvider provider = org.mockito.Mockito.mock(OAuthProvider.class);
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
  @DisplayName("callback: missing code returns 400")
  void callbackMissingCodeReturns400() throws Exception {
    when(oauthProviderRegistry.require("google")).thenReturn(org.mockito.Mockito.mock(OAuthProvider.class));
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
  @DisplayName("callback: success returns 302 to app redirect URL")
  void callbackSuccessReturnsRedirect() throws Exception {
    when(oauthProviderRegistry.require("google")).thenReturn(org.mockito.Mockito.mock(OAuthProvider.class));
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
}
