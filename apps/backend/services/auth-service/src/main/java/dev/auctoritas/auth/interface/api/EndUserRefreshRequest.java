package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserRefreshRequest(@NotBlank String refreshToken) {}
