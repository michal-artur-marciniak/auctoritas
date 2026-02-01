package dev.auctoritas.auth.interface.api;

public record InternalMicrosoftAuthorizeResponse(
    String clientId, String authorizationEndpoint, String scope) {}
