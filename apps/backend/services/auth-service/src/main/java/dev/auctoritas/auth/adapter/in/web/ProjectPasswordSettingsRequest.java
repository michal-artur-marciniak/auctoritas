package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProjectPasswordSettingsRequest(
    @NotNull @Min(1) Integer minLength,
    @NotNull Boolean requireUppercase,
    @NotNull Boolean requireLowercase,
    @NotNull Boolean requireNumbers,
    @NotNull Boolean requireSpecialChars,
    @NotNull @Min(0) Integer passwordHistoryCount) {}
