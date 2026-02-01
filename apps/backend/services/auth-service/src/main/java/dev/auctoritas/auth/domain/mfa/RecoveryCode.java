package dev.auctoritas.auth.domain.mfa;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Value object representing a recovery code.
 * Stores the hash of the code (not the plain code) and tracks usage.
 */
public record RecoveryCode(String codeHash, boolean used) {

  /**
   * Creates a RecoveryCode with a pre-computed hash.
   *
   * @param codeHash the SHA-256 hash of the recovery code (Base64 encoded)
   * @param used whether the code has been used
   * @throws DomainValidationException if codeHash is null or empty
   */
  public RecoveryCode {
    if (codeHash == null || codeHash.isEmpty()) {
      throw new DomainValidationException("recovery_code_hash_required");
    }
  }

  /**
   * Creates a RecoveryCode from a plain code, computing its hash.
   * Use this when creating new recovery codes.
   *
   * @param plainCode the plain recovery code (shown to user once)
   * @return RecoveryCode with computed hash
   */
  public static RecoveryCode fromPlainCode(String plainCode) {
    if (plainCode == null || plainCode.trim().isEmpty()) {
      throw new DomainValidationException("recovery_code_required");
    }
    String hash = hashCode(plainCode.trim());
    return new RecoveryCode(hash, false);
  }

  /**
   * Creates a RecoveryCode from an existing hash (used when loading from DB).
   *
   * @param codeHash the SHA-256 hash (Base64 encoded)
   * @return RecoveryCode
   */
  public static RecoveryCode fromHash(String codeHash) {
    return new RecoveryCode(codeHash, false);
  }

  /**
   * Verifies if a plain code matches this recovery code.
   *
   * @param plainCode the plain code to verify
   * @return true if the code matches
   */
  public boolean verify(String plainCode) {
    if (used) {
      return false;
    }
    String inputHash = hashCode(plainCode);
    return codeHash.equals(inputHash);
  }

  /**
   * Returns a new RecoveryCode marked as used.
   *
   * @return used RecoveryCode
   */
  public RecoveryCode markUsed() {
    return new RecoveryCode(codeHash, true);
  }

  private static String hashCode(String code) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  @Override
  public String toString() {
    return "RecoveryCode[hash=" + codeHash.substring(0, Math.min(16, codeHash.length())) + "..., used=" + used + "]";
  }
}
