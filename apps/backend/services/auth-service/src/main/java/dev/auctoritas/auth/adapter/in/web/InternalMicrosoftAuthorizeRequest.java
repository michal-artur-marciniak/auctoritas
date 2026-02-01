package dev.auctoritas.auth.adapter.in.web;

import java.util.UUID;

public record InternalMicrosoftAuthorizeRequest(
    UUID projectId, String redirectUri, String state, String codeVerifier) {}
