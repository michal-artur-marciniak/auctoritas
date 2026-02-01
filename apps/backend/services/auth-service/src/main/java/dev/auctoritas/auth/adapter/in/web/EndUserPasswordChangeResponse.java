package dev.auctoritas.auth.adapter.in.web;

public record EndUserPasswordChangeResponse(
    String message,
    boolean keptCurrentSession,
    boolean revokedOtherSessions) {}
