package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record EndUserEmailVerificationRequest(
    @NotBlank String token,
    @NotBlank String code) {}
