package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectCreateRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String slug) {}
