package dev.auctoritas.auth.api;

import dev.auctoritas.auth.domain.apikey.ApiKeyStatus;
import java.time.Instant;
import java.util.UUID;

public record ApiKeySecretResponse(
    UUID id,
    String name,
    String prefix,
    String key,
    ApiKeyStatus status,
    Instant createdAt) {}
