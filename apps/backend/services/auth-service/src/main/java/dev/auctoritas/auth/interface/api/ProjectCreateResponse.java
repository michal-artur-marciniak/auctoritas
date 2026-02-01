package dev.auctoritas.auth.interface.api;

public record ProjectCreateResponse(
    ProjectSummaryResponse project,
    ApiKeySecretResponse defaultApiKey) {}
