package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordResetRequest(
    @NotBlank String token,
    @NotBlank String newPassword) {}
