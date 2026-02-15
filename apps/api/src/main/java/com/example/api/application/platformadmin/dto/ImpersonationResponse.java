package com.example.api.application.platformadmin.dto;

import java.time.Instant;

/**
 * Response DTO for organization impersonation.
 * Contains the org-scoped token for impersonating an organization.
 */
public record ImpersonationResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String organizationId,
        String organizationName,
        String impersonatedBy,
        Instant expiresAt
) {
    public static ImpersonationResponse from(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String organizationId,
            String organizationName,
            String impersonatedBy,
            Instant expiresAt) {
        return new ImpersonationResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                organizationId,
                organizationName,
                impersonatedBy,
                expiresAt
        );
    }
}
