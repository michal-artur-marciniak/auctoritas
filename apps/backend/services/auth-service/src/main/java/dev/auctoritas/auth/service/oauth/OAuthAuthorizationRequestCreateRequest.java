package dev.auctoritas.auth.service.oauth;

import java.util.UUID;

public record OAuthAuthorizationRequestCreateRequest(
    UUID projectId, String redirectUri, String state, String codeVerifier) {}
