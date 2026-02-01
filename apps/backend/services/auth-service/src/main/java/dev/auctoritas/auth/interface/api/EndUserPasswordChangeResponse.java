package dev.auctoritas.auth.interface.api;

public record EndUserPasswordChangeResponse(
    String message,
    boolean keptCurrentSession,
    boolean revokedOtherSessions) {}
