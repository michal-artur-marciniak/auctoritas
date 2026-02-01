package dev.auctoritas.auth.application.oauth;

public record OAuthCallbackHandleRequest(String code, String state, String callbackUri) {}
