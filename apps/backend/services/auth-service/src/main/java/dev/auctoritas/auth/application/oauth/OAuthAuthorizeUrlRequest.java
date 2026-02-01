package dev.auctoritas.auth.application.oauth;

/** Inputs used to generate an OAuth provider authorize URL (PKCE supported). */
public record OAuthAuthorizeUrlRequest(
    String callbackUri, String state, String codeChallenge, String codeChallengeMethod) {}
