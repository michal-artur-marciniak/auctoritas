package dev.auctoritas.auth.interface.api;

public record InternalGoogleCallbackRequest(String code, String state, String callbackUri) {}
