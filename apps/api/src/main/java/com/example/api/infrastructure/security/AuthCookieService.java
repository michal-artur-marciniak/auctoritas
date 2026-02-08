package com.example.api.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

/**
 * Issues and clears auth cookies for access/refresh tokens.
 */
@Component
public class AuthCookieService {

    private final AuthCookieProperties properties;

    public AuthCookieService(AuthCookieProperties properties) {
        this.properties = properties;
    }

    public void setAccessToken(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", buildCookie(
                properties.getAccessTokenName(),
                token,
                properties.getAccessMaxAgeSeconds()
        ).toString());
    }

    public void setRefreshToken(HttpServletResponse response, String token) {
        response.addHeader("Set-Cookie", buildCookie(
                properties.getRefreshTokenName(),
                token,
                properties.getRefreshMaxAgeSeconds()
        ).toString());
    }

    public void clearTokens(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie(
                properties.getAccessTokenName(),
                "",
                0
        ).toString());
        response.addHeader("Set-Cookie", buildCookie(
                properties.getRefreshTokenName(),
                "",
                0
        ).toString());
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return readCookie(request, properties.getAccessTokenName());
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, properties.getRefreshTokenName());
    }

    private ResponseCookie buildCookie(String name, String value, int maxAgeSeconds) {
        final var builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.isSecure())
                .path("/")
                .maxAge(maxAgeSeconds);

        final var domain = properties.getDomain();
        if (!domain.isBlank()) {
            builder.domain(domain);
        }

        return builder.sameSite(normalizeSameSite(properties.getSameSite())).build();
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        for (final var cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return Optional.ofNullable(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private String normalizeSameSite(String sameSite) {
        if (sameSite == null || sameSite.isBlank()) {
            return "Lax";
        }
        return switch (sameSite.trim().toLowerCase(Locale.ROOT)) {
            case "strict" -> "Strict";
            case "none" -> "None";
            default -> "Lax";
        };
    }
}
