package com.example.api.domain.user;

import java.util.Objects;

/**
 * Value object representing a hashed password.
 *
 * <p>The domain defines the password contract but delegates actual
 * encoding to a {@link PasswordEncoder} port so the domain stays
 * framework-independent.</p>
 */
public final class Password {

    private static final int MIN_LENGTH = 8;

    private final String hashedValue;

    private Password(String hashedValue) {
        this.hashedValue = Objects.requireNonNull(hashedValue, "Hashed password must not be null");
    }

    /**
     * Creates a new password from plain text, validating strength and hashing.
     *
     * @param plainText the raw password
     * @param encoder   the password encoder port
     * @return a new Password with the hashed value
     * @throws IllegalArgumentException if the password is too short
     */
    public static Password create(String plainText, PasswordEncoder encoder) {
        Objects.requireNonNull(plainText, "Password must not be null");
        Objects.requireNonNull(encoder, "Password encoder must not be null");

        if (plainText.length() < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    "Password must be at least %d characters".formatted(MIN_LENGTH)
            );
        }

        return new Password(encoder.encode(plainText));
    }

    /**
     * Reconstitutes a Password from an already-hashed value (e.g. from the database).
     */
    public static Password fromHash(String hashedValue) {
        return new Password(hashedValue);
    }

    /**
     * Checks whether the given plain text matches this hashed password.
     */
    public boolean matches(String plainText, PasswordEncoder encoder) {
        return encoder.matches(plainText, hashedValue);
    }

    public String hashedValue() {
        return hashedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Password other)) return false;
        return hashedValue.equals(other.hashedValue);
    }

    @Override
    public int hashCode() {
        return hashedValue.hashCode();
    }

    @Override
    public String toString() {
        return "Password[****]";
    }
}
