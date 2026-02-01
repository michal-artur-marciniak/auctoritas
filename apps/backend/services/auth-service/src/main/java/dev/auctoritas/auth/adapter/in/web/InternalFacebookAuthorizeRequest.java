package dev.auctoritas.auth.adapter.in.web;

import java.util.UUID;

public record InternalFacebookAuthorizeRequest(
    UUID projectId, String redirectUri, String state, String codeVerifier) {}
