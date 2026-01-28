package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;

public record InternalMicrosoftCallbackRequest(
    @NotBlank String code, @NotBlank String state, @NotBlank String callbackUri) {}
