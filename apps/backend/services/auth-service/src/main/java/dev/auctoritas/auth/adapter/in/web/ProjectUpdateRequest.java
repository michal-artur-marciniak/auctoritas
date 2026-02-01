package dev.auctoritas.auth.adapter.in.web;

import dev.auctoritas.auth.domain.project.ProjectStatus;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
    @Size(max = 100) String name,
    @Size(max = 50) String slug,
    ProjectStatus status) {}
