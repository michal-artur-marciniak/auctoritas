package dev.auctoritas.auth.interface.api;

import java.util.UUID;

public record InternalGoogleAuthorizeRequest(
    UUID projectId, String redirectUri, String state, String codeVerifier) {}
