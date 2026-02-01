package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.exception.DomainValidationException;

/**
 * Value object representing a TOTP code (6-digit code).
 * Immutable and validates format at construction.
 */
public record TotpCode(String value) {

  /**
   * Creates a TotpCode from a raw string, validating it is a 6-digit code.
   *
   * @param rawCode the raw TOTP code
   * @return validated TotpCode
   * @throws DomainValidationException if code is null, empty, or invalid format
   */
  public static TotpCode of(String rawCode) {
    if (rawCode == null || rawCode.trim().isEmpty()) {
      throw new DomainValidationException("totp_code_required");
    }

    String normalized = rawCode.trim();

    if (!isValidFormat(normalized)) {
      throw new DomainValidationException("totp_code_invalid_format");
    }

    return new TotpCode(normalized);
  }

  private static boolean isValidFormat(String code) {
    return code.matches("^\\d{6}$");
  }

  @Override
  public String toString() {
    return "TotpCode[hidden]";
  }
}
