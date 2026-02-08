package com.example.api.infrastructure.security;

import com.example.api.application.auth.OAuthLoginUseCase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles OAuth2 login success by issuing cookies and redirecting to the frontend.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthLoginUseCase oAuthLoginUseCase;
    private final OAuth2UserInfoMapper userInfoMapper;
    private final AuthCookieService authCookieService;
    private final GithubEmailClient githubEmailClient;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendRedirectUrl;

    public OAuth2AuthenticationSuccessHandler(
            OAuthLoginUseCase oAuthLoginUseCase,
            OAuth2UserInfoMapper userInfoMapper,
            AuthCookieService authCookieService,
            GithubEmailClient githubEmailClient,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${app.frontend.redirect-url:http://localhost:5173/oauth/callback}") String frontendRedirectUrl) {
        this.oAuthLoginUseCase = oAuthLoginUseCase;
        this.userInfoMapper = userInfoMapper;
        this.authCookieService = authCookieService;
        this.githubEmailClient = githubEmailClient;
        this.authorizedClientService = authorizedClientService;
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth authentication failed");
            return;
        }

        final var registrationId = oauthToken.getAuthorizedClientRegistrationId();
        final var oAuthUser = oauthToken.getPrincipal();
        final var userInfo = userInfoMapper.fromOauthUser(registrationId, oAuthUser);
        final var email = resolveEmail(registrationId, oauthToken, userInfo);

        final var authResponse = oAuthLoginUseCase.execute(email, userInfo.name());
        authCookieService.setAccessToken(response, authResponse.accessToken());
        authCookieService.setRefreshToken(response, authResponse.refreshToken());

        response.sendRedirect(frontendRedirectUrl);
    }

    private String resolveEmail(String registrationId,
                                OAuth2AuthenticationToken oauthToken,
                                OAuth2UserInfo userInfo) {
        if (!"github".equals(registrationId)) {
            return userInfo.email();
        }

        if (userInfo.email() != null && !userInfo.email().isBlank()) {
            return userInfo.email();
        }

        final var client = authorizedClientService.loadAuthorizedClient(registrationId, oauthToken.getName());
        if (client != null && client.getAccessToken() != null) {
            final var token = client.getAccessToken().getTokenValue();
            final var email = githubEmailClient.fetchPrimaryEmail(token).orElse(null);
            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        throw new IllegalArgumentException("GitHub account has no public email or verified email");
    }
}
