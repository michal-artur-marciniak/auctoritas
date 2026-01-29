package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordChangeRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword) {}
