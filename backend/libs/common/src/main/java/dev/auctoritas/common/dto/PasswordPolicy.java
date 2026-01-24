package dev.auctoritas.common.dto;

/**
 * Password policy configuration DTO.
 * Defines the rules for password validation.
 */
public record PasswordPolicy(
    int minLength,
    int maxLength,
    boolean requireUppercase,
    boolean requireLowercase,
    boolean requireNumbers,
    boolean requireSpecialChars,
    int minUniqueChars) {

  /**
   * Creates a default password policy with sensible defaults.
   */
  public static PasswordPolicy defaults() {
    return new PasswordPolicy(
        8,      // minLength
        128,    // maxLength
        true,   // requireUppercase
        true,   // requireLowercase
        true,   // requireNumbers
        true,   // requireSpecialChars
        4       // minUniqueChars
    );
  }

  /**
   * Creates a relaxed password policy (for development/testing).
   */
  public static PasswordPolicy relaxed() {
    return new PasswordPolicy(
        6,      // minLength
        128,    // maxLength
        false,  // requireUppercase
        false,  // requireLowercase
        false,  // requireNumbers
        false,  // requireSpecialChars
        1       // minUniqueChars
    );
  }
}
