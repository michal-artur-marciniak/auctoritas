package com.example.api.domain.user;

/**
 * Domain port for password encoding.
 *
 * <p>Infrastructure layer provides the actual implementation
 * (e.g. BCrypt via Spring Security).</p>
 */
public interface PasswordEncoder {

    /**
     * Encodes a raw password.
     */
    String encode(String rawPassword);

    /**
     * Checks if a raw password matches an encoded password.
     */
    boolean matches(String rawPassword, String encodedPassword);
}
