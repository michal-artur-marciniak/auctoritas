package dev.auctoritas.auth.adapter.in.web;

public record InternalMicrosoftAuthorizeResponse(
    String clientId, String authorizationEndpoint, String scope) {}
