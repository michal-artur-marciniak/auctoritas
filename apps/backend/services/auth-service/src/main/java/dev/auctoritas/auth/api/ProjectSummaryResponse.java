package dev.auctoritas.auth.api;

import dev.auctoritas.auth.domain.project.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public record ProjectSummaryResponse(
    UUID id,
    String name,
    String slug,
    ProjectStatus status,
    Instant createdAt,
    Instant updatedAt) {}
