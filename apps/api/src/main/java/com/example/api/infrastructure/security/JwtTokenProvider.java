package com.example.api.infrastructure.security;

import com.example.api.application.auth.TokenProvider;
import com.example.api.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT implementation of the {@link TokenProvider} port.
 */
@Component
public class JwtTokenProvider implements TokenProvider {

    private final String secret;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    public String generateAccessToken(User user) {
        final var now = new Date();
        final var expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getId().value())
                .claim("email", user.getEmail().value())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        final var now = new Date();
        final var expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(user.getId().value())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey())
                .compact();
    }

    @Override
    public boolean validateAccessToken(String token) {
        try {
            final var claims = parseClaims(token);
            return claims.getExpiration().after(new Date())
                    && !"refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        try {
            final var claims = parseClaims(token);
            return claims.getExpiration().after(new Date())
                    && "refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String getUserIdFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
