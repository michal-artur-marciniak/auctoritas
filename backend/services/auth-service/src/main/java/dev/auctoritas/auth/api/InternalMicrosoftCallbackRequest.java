package dev.auctoritas.auth.api;

public record InternalMicrosoftCallbackRequest(String code, String state, String callbackUri) {}
