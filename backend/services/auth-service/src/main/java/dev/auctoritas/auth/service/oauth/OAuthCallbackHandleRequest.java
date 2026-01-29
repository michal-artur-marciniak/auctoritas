package dev.auctoritas.auth.service.oauth;

public record OAuthCallbackHandleRequest(String code, String state, String callbackUri) {}
