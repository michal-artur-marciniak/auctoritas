package dev.auctoritas.auth.api;

public record EndUserPasswordChangeResponse(
    String message,
    boolean keptCurrentSession,
    boolean revokedOtherSessions) {}
