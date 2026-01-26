package dev.auctoritas.auth.api;

import dev.auctoritas.common.enums.ApiKeyStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record ApiKeySummaryResponse(
    UUID id,
    String name,
    String prefix,
    ApiKeyStatus status,
    LocalDateTime lastUsedAt,
    Instant createdAt) {}
