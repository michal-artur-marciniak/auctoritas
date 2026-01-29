package dev.auctoritas.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import dev.auctoritas.auth.service.oauth.OAuthProvider;
import dev.auctoritas.auth.service.oauth.OAuthProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {"auth.public-api-base-url=https://public.example/"})
@ActiveProfiles("test")
class PublicOAuthControllerPublicApiBaseUrlOverrideTest {

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
  @DisplayName("callback: auth.public-api-base-url override is used for callback URI")
  void callbackUsesPublicApiBaseUrlOverride() throws Exception {
    when(oauthProviderRegistry.require("google")).thenReturn(org.mockito.Mockito.mock(OAuthProvider.class));
    when(oauthGoogleCallbackService.handleCallback(eq("provider-code"), eq("state"), any(String.class)))
        .thenReturn("https://example.com/redirect");

    mockMvc
        .perform(
            get("/api/v1/auth/oauth/google/callback")
                .secure(true)
                .queryParam("code", "provider-code")
                .queryParam("state", "state"))
        .andExpect(status().isFound());

    ArgumentCaptor<String> callbackUriCaptor = ArgumentCaptor.forClass(String.class);
    verify(oauthGoogleCallbackService)
        .handleCallback(eq("provider-code"), eq("state"), callbackUriCaptor.capture());

    assertThat(callbackUriCaptor.getValue())
        .isEqualTo("https://public.example/api/v1/auth/oauth/google/callback");
  }
}
