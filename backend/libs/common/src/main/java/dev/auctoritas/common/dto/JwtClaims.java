package dev.auctoritas.common.dto;

import java.util.Set;

public record JwtClaims(
    String subject,
    String orgId,
    String projectId,
    String role,
    String type,
    Set<String> permissions,
    String issuer,
    long iat,
    long exp) {
}
