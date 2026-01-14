package dev.auctoritas.auth.dto;

import dev.auctoritas.common.enums.OrganizationStatus;
import java.time.Instant;
import java.util.UUID;

public record OrganizationDetailsResponse(
    UUID id,
    String name,
    String slug,
    OrganizationStatus status,
    int memberCount,
    int projectCount,
    Instant createdAt
) {}
