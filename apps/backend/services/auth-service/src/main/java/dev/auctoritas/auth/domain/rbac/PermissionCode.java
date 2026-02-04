package dev.auctoritas.auth.domain.rbac;

import dev.auctoritas.auth.domain.exception.DomainValidationException;

/**
 * Value object representing a validated permission code.
 */
public record PermissionCode(String value) {
  private static final int MAX_LENGTH = 100;
  private static final String VALID_PATTERN = "^[a-z]+:[a-z-]+$";

  /**
   * Creates a PermissionCode from a raw string, normalizing and validating it.
   *
   * @param rawCode the raw permission code string
   * @return validated PermissionCode
   * @throws DomainValidationException if code is null, empty, or invalid format
   */
  public static PermissionCode of(String rawCode) {
    if (rawCode == null || rawCode.trim().isEmpty()) {
      throw new DomainValidationException("permission_code_required");
    }

    String normalized = rawCode.trim().toLowerCase();

    if (normalized.length() > MAX_LENGTH) {
      throw new DomainValidationException("permission_code_too_long");
    }

    if (!normalized.matches(VALID_PATTERN)) {
      throw new DomainValidationException("permission_code_invalid_format");
    }

    return new PermissionCode(normalized);
  }

  @Override
  public String toString() {
    return value;
  }
}
