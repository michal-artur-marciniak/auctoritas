package dev.auctoritas.auth.service.oauth;

/** Provider-specific configuration needed to build an OAuth authorization URL. */
public record OAuthAuthorizeDetails(String clientId, String authorizationEndpoint, String scope) {}
