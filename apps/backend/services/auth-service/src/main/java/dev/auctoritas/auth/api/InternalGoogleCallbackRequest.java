package dev.auctoritas.auth.api;

public record InternalGoogleCallbackRequest(String code, String state, String callbackUri) {}
