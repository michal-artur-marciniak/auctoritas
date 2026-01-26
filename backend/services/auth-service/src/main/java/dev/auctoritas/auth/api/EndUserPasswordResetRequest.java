package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordResetRequest(
    @NotBlank String token,
    @NotBlank String newPassword) {}
