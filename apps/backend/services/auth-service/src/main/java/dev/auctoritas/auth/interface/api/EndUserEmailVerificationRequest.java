package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserEmailVerificationRequest(
    @NotBlank String token,
    @NotBlank String code) {}
