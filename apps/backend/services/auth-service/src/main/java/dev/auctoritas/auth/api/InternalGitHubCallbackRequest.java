package dev.auctoritas.auth.api;

public record InternalGitHubCallbackRequest(String code, String state, String callbackUri) {}
