package dev.auctoritas.common.dto;

import java.util.UUID;

public record JwtClaims(
    UUID subject, UUID organizationId, String role, String type, String issuer) {}
