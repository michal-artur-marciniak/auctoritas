package com.example.api.infrastructure.security;

import com.example.api.application.auth.org.OrgTokenProvider;
import com.example.api.domain.organization.OrganizationMember;
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

    public OrgJwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
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

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
