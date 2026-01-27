package dev.auctoritas.auth.api;

import java.util.UUID;

public record InternalFacebookAuthorizeRequest(
    UUID projectId, String redirectUri, String state, String codeVerifier) {}
