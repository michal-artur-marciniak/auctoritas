package dev.auctoritas.auth.adapter.in.web;

import java.time.Instant;
import java.util.UUID;

public record RoleSummaryResponse(
    UUID id,
    String name,
    String description,
    boolean system,
    int permissionCount,
    Instant createdAt,
    Instant updatedAt) {}
