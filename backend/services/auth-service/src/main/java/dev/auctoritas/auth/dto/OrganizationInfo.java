package dev.auctoritas.auth.dto;

import java.util.UUID;

public record OrganizationInfo(
    UUID id,
    String name,
    String slug
) {}
