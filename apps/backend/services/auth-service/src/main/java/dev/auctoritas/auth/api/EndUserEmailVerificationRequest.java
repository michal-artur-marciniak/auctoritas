package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserEmailVerificationRequest(
    @NotBlank String token,
    @NotBlank String code) {}
