package com.example.api.infrastructure.security;

import com.example.api.application.auth.org.OrgTokenProvider;
import com.example.api.domain.organization.OrganizationMember;
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
 * JWT token provider for organization member auth.
 */
@Component
public class OrgJwtTokenProvider implements OrgTokenProvider {

    private final String secret;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public OrgJwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.refresh-expiration}") long refreshExpirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    public String generateAccessToken(OrganizationMember member) {
        final var now = new Date();
        final var expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(member.getId().value())
                .claim("orgId", member.getOrganizationId().value())
                .claim("role", member.getRole().name())
                .claim("type", "org")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey())
                .compact();
    }

    @Override
    public String generateRefreshToken(OrganizationMember member) {
        final var now = new Date();
        final var expiry = new Date(now.getTime() + refreshExpirationMs);

        return Jwts.builder()
                .subject(member.getId().value())
                .claim("type", "org_refresh")
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
                    && "org".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean validateRefreshToken(String token) {
        try {
            final var claims = parseClaims(token);
            return claims.getExpiration().after(new Date())
                    && "org_refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String getMemberIdFromToken(String token) {
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
