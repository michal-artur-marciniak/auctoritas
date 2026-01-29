package dev.auctoritas.auth.api;

public record InternalAppleCallbackRequest(String code, String state, String callbackUri) {}
