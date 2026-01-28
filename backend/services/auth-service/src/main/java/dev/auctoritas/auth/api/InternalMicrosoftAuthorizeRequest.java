package dev.auctoritas.auth.api;

import java.util.UUID;

public record InternalMicrosoftAuthorizeRequest(
    UUID projectId, String redirectUri, String state, String codeVerifier) {}
