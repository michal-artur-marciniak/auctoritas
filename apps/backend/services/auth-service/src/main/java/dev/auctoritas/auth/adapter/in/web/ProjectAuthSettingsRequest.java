package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotNull;

public record ProjectAuthSettingsRequest(@NotNull Boolean requireVerifiedEmailForLogin) {}
