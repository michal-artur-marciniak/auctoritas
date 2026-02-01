package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordResetRequest(
    @NotBlank String token,
    @NotBlank String newPassword) {}
