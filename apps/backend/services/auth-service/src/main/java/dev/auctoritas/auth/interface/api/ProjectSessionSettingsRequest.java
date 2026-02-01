package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProjectSessionSettingsRequest(
    @NotNull @Min(1) Integer accessTokenTtlSeconds,
    @NotNull @Min(1) Integer refreshTokenTtlSeconds,
    @NotNull @Min(1) Integer maxSessions,
    @NotNull Boolean mfaEnabled,
    @NotNull Boolean mfaRequired) {}
