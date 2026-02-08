package com.example.api.infrastructure.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles OAuth2 login failures and redirects to the frontend with error details.
 */
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String frontendRedirectUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.frontend.redirect-url:http://localhost:5173/oauth/callback}") String frontendRedirectUrl) {
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        final var errorMessage = exception.getMessage() == null ? "OAuth authentication failed" : exception.getMessage();
        final var redirectUrl = frontendRedirectUrl
                + "?error=" + urlEncode(errorMessage);
        response.sendRedirect(redirectUrl);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
