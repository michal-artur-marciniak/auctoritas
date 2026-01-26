package dev.auctoritas.auth.api;

import java.util.Map;

public record ProjectSettingsResponse(
    int minLength,
    boolean requireUppercase,
    boolean requireNumbers,
    boolean requireSpecialChars,
    int passwordHistoryCount,
    int accessTokenTtlSeconds,
    int refreshTokenTtlSeconds,
    int maxSessions,
    boolean mfaEnabled,
    boolean mfaRequired,
    Map<String, Object> oauthConfig) {}
