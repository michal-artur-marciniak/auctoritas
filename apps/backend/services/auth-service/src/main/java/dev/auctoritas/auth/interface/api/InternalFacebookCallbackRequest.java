package dev.auctoritas.auth.interface.api;

public record InternalFacebookCallbackRequest(String code, String state, String callbackUri) {}
