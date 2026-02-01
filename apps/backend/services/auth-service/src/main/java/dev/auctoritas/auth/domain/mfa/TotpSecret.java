package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.exception.DomainValidationException;

/**
 * Value object representing an encrypted TOTP secret.
 * Immutable and validates the encrypted format.
 */
public record TotpSecret(String encryptedValue) {

  /**
   * Creates a TotpSecret with an encrypted value.
   *
   * @param encryptedValue the AES-256 encrypted secret
   * @throws DomainValidationException if encrypted value is null or empty
   */
  public TotpSecret {
    if (encryptedValue == null || encryptedValue.isEmpty()) {
      throw new DomainValidationException("totp_secret_required");
    }
    if (encryptedValue.length() > 500) {
      throw new DomainValidationException("totp_secret_too_long");
    }
  }

  /**
   * Creates a TotpSecret from an encrypted string.
   *
   * @param encryptedValue the encrypted secret value
   * @return validated TotpSecret
   */
  public static TotpSecret of(String encryptedValue) {
    return new TotpSecret(encryptedValue);
  }

  @Override
  public String toString() {
    return "TotpSecret[encrypted]";
  }
}
