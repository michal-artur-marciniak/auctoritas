package dev.auctoritas.auth.interface.api;

public record InternalGitHubCallbackRequest(String code, String state, String callbackUri) {}
