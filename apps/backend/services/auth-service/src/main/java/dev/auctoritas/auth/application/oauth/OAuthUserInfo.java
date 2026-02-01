package dev.auctoritas.auth.application.oauth;

/** Normalized provider user identity fields needed for account linking. */
public record OAuthUserInfo(
    String providerUserId, String email, Boolean emailVerified, String name) {}
