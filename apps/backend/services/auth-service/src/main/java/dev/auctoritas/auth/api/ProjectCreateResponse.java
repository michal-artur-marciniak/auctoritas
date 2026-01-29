package dev.auctoritas.auth.api;

public record ProjectCreateResponse(
    ProjectSummaryResponse project,
    ApiKeySecretResponse defaultApiKey) {}
