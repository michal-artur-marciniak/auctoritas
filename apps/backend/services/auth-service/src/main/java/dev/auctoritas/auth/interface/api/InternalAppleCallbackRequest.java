package dev.auctoritas.auth.interface.api;

public record InternalAppleCallbackRequest(String code, String state, String callbackUri) {}
