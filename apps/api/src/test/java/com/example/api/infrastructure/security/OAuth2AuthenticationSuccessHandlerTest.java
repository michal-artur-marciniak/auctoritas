package com.example.api.infrastructure.security;

import com.example.api.application.auth.OAuthLoginUseCase;
import com.example.api.application.auth.dto.AuthResponse;
import com.example.api.application.auth.dto.UserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private OAuthLoginUseCase oAuthLoginUseCase;

    @Mock
    private OAuth2UserInfoMapper userInfoMapper;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private GithubEmailClient githubEmailClient;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                oAuthLoginUseCase,
                userInfoMapper,
                authCookieService,
                githubEmailClient,
                authorizedClientService,
                "http://localhost/redirect"
        );
    }

    @Test
    void fetchesGithubEmailWhenMissing() throws Exception {
        final var principal = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("name", "GitHub User"),
                "name"
        );
        final var authentication = new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

        final var registration = ClientRegistration.withRegistrationId("github")
                .clientId("client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();

        final var token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
        final var authorizedClient = new OAuth2AuthorizedClient(registration, authentication.getName(), token);

        when(userInfoMapper.fromOauthUser("github", principal))
                .thenReturn(new OAuth2UserInfo("", "GitHub User"));
        when(authorizedClientService.loadAuthorizedClient("github", authentication.getName()))
                .thenReturn(authorizedClient);
        when(githubEmailClient.fetchPrimaryEmail("token"))
                .thenReturn(Optional.of("user@example.com"));
        when(oAuthLoginUseCase.execute("user@example.com", "GitHub User"))
                .thenReturn(new AuthResponse(
                        "access",
                        "refresh",
                        new UserDto("user-id", "user@example.com", "User", "USER")
                ));

        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authCookieService).setAccessToken(response, "access");
        verify(authCookieService).setRefreshToken(response, "refresh");
        verify(oAuthLoginUseCase).execute("user@example.com", "GitHub User");
        assertEquals("http://localhost/redirect", response.getRedirectedUrl());
    }
}
