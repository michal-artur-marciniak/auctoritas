package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordChangeRequest(
    @NotBlank String currentPassword,
    @NotBlank String newPassword) {}
