package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.NotBlank;

public record OrgRefreshRequest(@NotBlank String refreshToken) {}
