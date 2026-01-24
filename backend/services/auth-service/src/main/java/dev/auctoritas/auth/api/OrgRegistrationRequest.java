package dev.auctoritas.auth.api;

public record OrgRegistrationRequest(
    String orgName,
    String slug,
    String ownerEmail,
    String ownerPassword,
    String ownerName) {}
