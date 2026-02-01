package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.NotNull;

public record ProjectAuthSettingsRequest(@NotNull Boolean requireVerifiedEmailForLogin) {}
