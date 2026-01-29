package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotBlank;

public record OrgRefreshRequest(@NotBlank String refreshToken) {}
