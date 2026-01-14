package dev.auctoritas.auth.dto;

import dev.auctoritas.auth.entity.organization.OrganizationMember;
import java.time.Instant;
import java.util.UUID;

public record RegistrationResult(
    boolean success,
    String message,
    UUID organizationId,
    UUID memberId,
    String organizationName,
    String organizationSlug,
    String email,
    String name,
    Instant createdAt
) {
    public static RegistrationResult success(
        OrganizationMember member,
        UUID organizationId,
        String organizationName,
        String organizationSlug,
        Instant createdAt
    ) {
        return new RegistrationResult(
            true,
            "Organization registered successfully",
            organizationId,
            member.getId(),
            organizationName,
            organizationSlug,
            member.getEmail(),
            member.getName(),
            createdAt
        );
    }

    public static RegistrationResult failure(String message) {
        return new RegistrationResult(false, message, null, null, null, null, null, null, null);
    }
}
