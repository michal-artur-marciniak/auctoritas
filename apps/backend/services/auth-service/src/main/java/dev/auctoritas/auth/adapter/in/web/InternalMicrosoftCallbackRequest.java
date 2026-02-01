package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record InternalMicrosoftCallbackRequest(
    @NotBlank String code, @NotBlank String state, @NotBlank String callbackUri) {}
