package com.example.api.application.platformadmin;

import com.example.api.application.platformadmin.dto.ImpersonationResponse;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationMemberRole;
import com.example.api.domain.organization.OrganizationRepository;
import com.example.api.domain.organization.OrganizationStatus;
import com.example.api.domain.organization.exception.OrganizationNotFoundException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * Use case for platform admin to impersonate an organization.
 * Generates an org-scoped token with impersonation metadata.
 */
@Component
public class ImpersonateOrganizationUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ImpersonateOrganizationUseCase.class);
    private static final long IMPERSONATION_TOKEN_EXPIRATION_MS = 15 * 60 * 1000; // 15 minutes

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final String jwtSecret;

    public ImpersonateOrganizationUseCase(
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            @Value("${jwt.secret}") String jwtSecret) {
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.jwtSecret = jwtSecret;
    }

    @Transactional(readOnly = true)
    public ImpersonationResponse execute(String organizationId, String impersonatedByAdminId) {
        final var orgId = OrganizationId.of(organizationId);

        final var organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        final var members = organizationMemberRepository.findByOrganizationId(orgId);
        if (members.isEmpty()) {
            throw new IllegalStateException("Cannot impersonate organization with no members");
        }

        // Find an OWNER to impersonate, or fallback to any active member
        final var memberToImpersonate = findMemberToImpersonate(members);

        final var now = Instant.now();
        final var expiresAt = now.plus(IMPERSONATION_TOKEN_EXPIRATION_MS, ChronoUnit.MILLIS);

        final var accessToken = generateImpersonationToken(
                memberToImpersonate,
                impersonatedByAdminId,
                now,
                expiresAt
        );

        final var refreshToken = generateImpersonationRefreshToken(
                memberToImpersonate,
                impersonatedByAdminId,
                now
        );

        // Log impersonation for audit
        logger.info("Organization impersonation: admin={} impersonating org={} (member={})",
                impersonatedByAdminId,
                organizationId,
                memberToImpersonate.getId().value());

        return ImpersonationResponse.from(
                accessToken,
                refreshToken,
                IMPERSONATION_TOKEN_EXPIRATION_MS / 1000,
                organization.getId().value(),
                organization.getName(),
                impersonatedByAdminId,
                expiresAt
        );
    }

    private OrganizationMember findMemberToImpersonate(List<OrganizationMember> members) {
        return members.stream()
                .filter(m -> m.getRole() == OrganizationMemberRole.OWNER)
                .filter(m -> m.getStatus() == OrganizationStatus.ACTIVE)
                .findFirst()
                .orElseGet(() -> members.stream()
                        .filter(m -> m.getStatus() == OrganizationStatus.ACTIVE)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No active members found in organization")));
    }

    private String generateImpersonationToken(
            OrganizationMember member,
            String impersonatedBy,
            Instant issuedAt,
            Instant expiresAt) {

        return Jwts.builder()
                .subject(member.getId().value())
                .claim("orgId", member.getOrganizationId().value())
                .claim("role", member.getRole().name())
                .claim("type", "org")
                .claim("impersonatedBy", impersonatedBy)
                .claim("impersonated", true)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey())
                .compact();
    }

    private String generateImpersonationRefreshToken(
            OrganizationMember member,
            String impersonatedBy,
            Instant issuedAt) {

        final var refreshExpiresAt = issuedAt.plus(IMPERSONATION_TOKEN_EXPIRATION_MS * 2, ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(member.getId().value())
                .claim("type", "org_refresh")
                .claim("impersonatedBy", impersonatedBy)
                .claim("impersonated", true)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(refreshExpiresAt))
                .signWith(secretKey())
                .compact();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
