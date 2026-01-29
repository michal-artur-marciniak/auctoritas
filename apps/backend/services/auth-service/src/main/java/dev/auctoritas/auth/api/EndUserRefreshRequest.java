package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserRefreshRequest(@NotBlank String refreshToken) {}
