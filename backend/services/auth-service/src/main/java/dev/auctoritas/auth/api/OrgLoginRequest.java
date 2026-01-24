package dev.auctoritas.auth.api;

public record OrgLoginRequest(
    String orgSlug,
    String email,
    String password) {}
