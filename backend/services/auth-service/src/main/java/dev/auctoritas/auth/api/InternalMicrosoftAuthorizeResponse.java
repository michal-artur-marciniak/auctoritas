package dev.auctoritas.auth.api;

public record InternalMicrosoftAuthorizeResponse(
    String clientId, String authorizationEndpoint, String scope) {}
