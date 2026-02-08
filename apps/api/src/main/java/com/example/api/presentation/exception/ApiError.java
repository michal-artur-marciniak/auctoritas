package com.example.api.presentation.exception;

import java.time.Instant;

/**
 * Standard API error response.
 */
public record ApiError(int status, String message, Instant timestamp) {

    public ApiError(int status, String message) {
        this(status, message, Instant.now());
    }
}
