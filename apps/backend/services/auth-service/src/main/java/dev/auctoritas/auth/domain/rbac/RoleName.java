package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.exception.DomainValidationException;

/**
 * Value object representing a validated role name.
 */
public record RoleName(String value) {
  private static final int MAX_LENGTH = 50;
  private static final String VALID_PATTERN = "^[A-Za-z0-9][A-Za-z0-9 _-]*$";

  /**
   * Creates a RoleName from a raw string, normalizing and validating it.
   *
   * @param rawName the raw role name string
   * @return validated RoleName
   * @throws DomainValidationException if name is null, empty, or invalid format
   */
  public static RoleName of(String rawName) {
    if (rawName == null || rawName.trim().isEmpty()) {
      throw new DomainValidationException("role_name_required");
    }

    String normalized = rawName.trim();

    if (normalized.length() > MAX_LENGTH) {
      throw new DomainValidationException("role_name_too_long");
    }

    if (!normalized.matches(VALID_PATTERN)) {
      throw new DomainValidationException("role_name_invalid_format");
    }

    return new RoleName(normalized);
  }

  @Override
  public String toString() {
    return value;
  }
}
