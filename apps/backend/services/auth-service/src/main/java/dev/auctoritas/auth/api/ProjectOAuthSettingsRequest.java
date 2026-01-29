package dev.auctoritas.auth.api;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ProjectOAuthSettingsRequest(@NotNull Map<String, Object> config) {}
