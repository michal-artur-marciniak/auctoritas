package dev.auctoritas.auth.domain.model.enduser;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.Objects;

/**
 * Value object representing a validated email address.
 * Immutable and validates format at construction.
 */
public final class Email {
  private final String value;

  private Email(String value) {
    this.value = Objects.requireNonNull(value, "email cannot be null");
  }

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

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Email email = (Email) o;
    return Objects.equals(value, email.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}
