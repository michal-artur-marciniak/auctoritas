package com.example.api.application.auth;

import com.example.api.domain.user.User;

/**
 * Port for token generation and validation.
 *
 * <p>Infrastructure layer provides the actual JWT implementation.</p>
 */
public interface TokenProvider {

    /**
     * Generates an access token for the given user.
     */
    String generateAccessToken(User user);

    /**
     * Generates a refresh token for the given user.
     */
    String generateRefreshToken(User user);

    /**
     * Validates an access token and returns true if valid.
     */
    boolean validateAccessToken(String token);

    /**
     * Validates a refresh token and returns true if valid.
     */
    boolean validateRefreshToken(String token);

    /**
     * Extracts the user ID from a token.
     */
    String getUserIdFromToken(String token);
}
