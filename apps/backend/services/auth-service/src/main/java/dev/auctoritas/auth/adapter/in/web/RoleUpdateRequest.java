package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.Size;

public record RoleUpdateRequest(
    @Size(max = 50) String name,
    @Size(max = 255) String description) {}
