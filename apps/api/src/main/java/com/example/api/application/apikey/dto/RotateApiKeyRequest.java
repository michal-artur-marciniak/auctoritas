package com.example.api.application.apikey.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rotating an API key.
 */
public record RotateApiKeyRequest(
    @NotBlank(message = "Environment ID is required")
    String environmentId
) {}
