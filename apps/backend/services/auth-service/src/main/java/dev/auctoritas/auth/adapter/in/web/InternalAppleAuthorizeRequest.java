package dev.auctoritas.auth.adapter.in.web;

import java.util.UUID;

public record InternalAppleAuthorizeRequest(UUID projectId, String redirectUri, String state, String codeVerifier) {}
