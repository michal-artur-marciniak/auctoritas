package dev.auctoritas.auth.adapter.in.web;

public record ProjectCreateResponse(
    ProjectSummaryResponse project,
    ApiKeySecretResponse defaultApiKey) {}
