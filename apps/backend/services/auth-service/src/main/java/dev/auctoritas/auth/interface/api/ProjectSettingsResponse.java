package dev.auctoritas.auth.interface.api;

import java.util.Map;

public record ProjectSettingsResponse(
    int minLength,
    boolean requireUppercase,
    boolean requireLowercase,
    boolean requireNumbers,
    boolean requireSpecialChars,
    int passwordHistoryCount,
    int accessTokenTtlSeconds,
    int refreshTokenTtlSeconds,
    int maxSessions,
    boolean requireVerifiedEmailForLogin,
    boolean mfaEnabled,
    boolean mfaRequired,
    Map<String, Object> oauthConfig) {}
