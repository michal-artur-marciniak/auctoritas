package com.example.api.presentation.auth.sdk;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when API key is missing or invalid for SDK endpoints.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class MissingApiKeyException extends RuntimeException {

    public MissingApiKeyException() {
        super("Valid API key required");
    }
}
