package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordChangeRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword) {}
