package dev.auctoritas.auth.api;

public record InternalFacebookCallbackRequest(String code, String state, String callbackUri) {}
