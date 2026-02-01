package dev.auctoritas.auth.application.port.out.security;

/**
 * Port for encrypting and decrypting sensitive data (like TOTP secrets).
 * Uses AES-256-GCM for authenticated encryption.
 */
public interface EncryptionPort {

  /**
   * Encrypts plaintext using AES-256-GCM.
   *
   * @param plaintext the data to encrypt
   * @return Base64-encoded encrypted data with IV and authentication tag
   */
  String encrypt(String plaintext);

  /**
   * Decrypts ciphertext that was encrypted with AES-256-GCM.
   *
   * @param ciphertext Base64-encoded encrypted data with IV and authentication tag
   * @return the decrypted plaintext
   * @throws SecurityException if decryption fails (invalid key, tampered data, etc.)
   */
  String decrypt(String ciphertext);

  /**
   * Generates a cryptographically secure random TOTP secret.
   *
   * @return Base32-encoded random secret (suitable for authenticator apps)
   */
  String generateTotpSecret();

  /**
   * Generates recovery codes for MFA backup.
   *
   * @param count number of codes to generate
   * @return array of plain recovery codes (should be shown to user once)
   */
  String[] generateRecoveryCodes(int count);
}
