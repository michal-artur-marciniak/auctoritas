package dev.auctoritas.auth.interface.api;

import java.util.UUID;

public record InternalAppleAuthorizeRequest(UUID projectId, String redirectUri, String state, String codeVerifier) {}
