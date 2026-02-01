package dev.auctoritas.auth.adapter.in.web;

public record InternalGitHubCallbackRequest(String code, String state, String callbackUri) {}
