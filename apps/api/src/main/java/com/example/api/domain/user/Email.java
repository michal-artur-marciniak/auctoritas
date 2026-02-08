package com.example.api.domain.user;

import com.example.api.domain.user.exception.InvalidEmailException;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a validated email address.
 */
public record Email(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    public Email {
        Objects.requireNonNull(value, "Email must not be null");
        final var normalized = value.strip().toLowerCase();
        if (!isValid(normalized)) {
            throw new InvalidEmailException(value);
        }
        value = normalized;
    }

    private static boolean isValid(String email) {
        return !email.isBlank() && EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
