package dev.auctoritas.auth.domain.valueobject;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.Objects;

/**
 * Value object representing a validated password.
 * Immutable and validates against project policy at construction.
 */
public final class Password {
  private static final int MAX_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;

  private final String value;
  private final boolean hashed;

  private Password(String value, boolean hashed) {
    this.value = Objects.requireNonNull(value, "password cannot be null");
    this.hashed = hashed;
  }

  /**
   * Creates a Password from a raw plain text string with validation.
   *
   * @param rawPassword the raw password
   * @param minLength minimum length requirement
   * @param requireUppercase require uppercase letters
   * @param requireLowercase require lowercase letters
   * @param requireNumbers require numeric digits
   * @param requireSpecialChars require special characters
   * @return validated Password (plain text, not yet hashed)
   * @throws DomainValidationException if password doesn't meet policy
   */
  public static Password create(
      String rawPassword,
      int minLength,
      boolean requireUppercase,
      boolean requireLowercase,
      boolean requireNumbers,
      boolean requireSpecialChars) {

    if (rawPassword == null || rawPassword.isEmpty()) {
      throw new DomainValidationException("password_required");
    }

    if (rawPassword.length() < minLength) {
      throw new DomainValidationException("password_too_short");
    }

    if (rawPassword.length() > MAX_LENGTH) {
      throw new DomainValidationException("password_too_long");
    }

    if (requireUppercase && !rawPassword.matches(".*[A-Z].*")) {
      throw new DomainValidationException("password_missing_uppercase");
    }

    if (requireLowercase && !rawPassword.matches(".*[a-z].*")) {
      throw new DomainValidationException("password_missing_lowercase");
    }

    if (requireNumbers && !rawPassword.matches(".*[0-9].*")) {
      throw new DomainValidationException("password_missing_number");
    }

    if (requireSpecialChars && !rawPassword.matches(".*[^A-Za-z0-9].*")) {
      throw new DomainValidationException("password_missing_special_char");
    }

    int minUnique = Math.max(1, Math.min(DEFAULT_MIN_UNIQUE, minLength));
    long uniqueChars = rawPassword.chars().distinct().count();
    if (uniqueChars < minUnique) {
      throw new DomainValidationException("password_insufficient_unique_chars");
    }

    return new Password(rawPassword, false);
  }

  /**
   * Wraps an already-hashed password.
   *
   * @param hashedPassword the hashed password value
   * @return Password marked as hashed
   */
  public static Password fromHash(String hashedPassword) {
    if (hashedPassword == null || hashedPassword.isEmpty()) {
      throw new DomainValidationException("password_hash_required");
    }
    return new Password(hashedPassword, true);
  }

  public String value() {
    return value;
  }

  public boolean isHashed() {
    return hashed;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Password password = (Password) o;
    return hashed == password.hashed && Objects.equals(value, password.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, hashed);
  }

  @Override
  public String toString() {
    return hashed ? "[HASHED]" : "[PLAIN_TEXT_HIDDEN]";
  }
}
