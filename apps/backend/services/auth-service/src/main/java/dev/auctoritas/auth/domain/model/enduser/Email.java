package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.exception.DomainValidationException;

/**
 * Value object representing a validated email address.
 * Immutable and validates format at construction.
 */
public record Email(String value) {

  /**
   * Creates an Email from a raw string, normalizing and validating it.
   *
   * @param rawEmail the raw email string
   * @return validated Email
   * @throws DomainValidationException if email is null, empty, or invalid format
   */
  public static Email of(String rawEmail) {
    if (rawEmail == null || rawEmail.trim().isEmpty()) {
      throw new DomainValidationException("email_required");
    }

    String normalized = rawEmail.trim().toLowerCase();

    if (!isValidFormat(normalized)) {
      throw new DomainValidationException("email_invalid_format");
    }

    return new Email(normalized);
  }

  private static boolean isValidFormat(String email) {
    return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
  }

  @Override
  public String toString() {
    return value;
  }
}
