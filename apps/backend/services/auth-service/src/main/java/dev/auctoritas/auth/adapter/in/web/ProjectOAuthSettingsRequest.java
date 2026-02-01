package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ProjectOAuthSettingsRequest(@NotNull Map<String, Object> config) {}
