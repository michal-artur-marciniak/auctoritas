package dev.auctoritas.auth.api;

import dev.auctoritas.common.enums.ApiKeyEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApiKeyCreateRequest(
    @NotBlank @Size(max = 50) String name,
    ApiKeyEnvironment environment) {}
