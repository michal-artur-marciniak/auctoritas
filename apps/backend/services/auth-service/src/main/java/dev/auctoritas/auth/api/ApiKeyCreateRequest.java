package dev.auctoritas.auth.api;

import dev.auctoritas.auth.domain.apikey.ApiKeyEnvironment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ApiKeyCreateRequest(
    @NotBlank @Size(max = 50) String name,
    @NotNull ApiKeyEnvironment environment) {}
