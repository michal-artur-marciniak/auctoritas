package dev.auctoritas.auth.api;

import dev.auctoritas.common.enums.ProjectStatus;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
    @Size(max = 100) String name,
    @Size(max = 50) String slug,
    ProjectStatus status) {}
