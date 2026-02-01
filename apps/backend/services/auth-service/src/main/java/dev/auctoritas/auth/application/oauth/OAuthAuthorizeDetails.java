package dev.auctoritas.auth.application.oauth;

/** Provider-specific configuration needed to build an OAuth authorization URL. */
public record OAuthAuthorizeDetails(String clientId, String authorizationEndpoint, String scope) {}
