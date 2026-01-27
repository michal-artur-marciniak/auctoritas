package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotNull;

public record ProjectAuthSettingsRequest(@NotNull Boolean requireVerifiedEmailForLogin) {}
