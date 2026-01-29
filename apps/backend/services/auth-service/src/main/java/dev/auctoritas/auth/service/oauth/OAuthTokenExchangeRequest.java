package dev.auctoritas.auth.service.oauth;

/** Inputs used to exchange an authorization code for provider user info. */
public record OAuthTokenExchangeRequest(String code, String callbackUri, String codeVerifier) {}
