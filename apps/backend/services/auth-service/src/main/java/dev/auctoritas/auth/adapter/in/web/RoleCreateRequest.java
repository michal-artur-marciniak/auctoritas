package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleCreateRequest(
    @NotBlank @Size(max = 50) String name,
    @Size(max = 255) String description) {}
