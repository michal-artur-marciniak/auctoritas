package dev.auctoritas.auth.domain.project;

import dev.auctoritas.auth.domain.exception.DomainValidationException;

/**
 * Value object representing MFA policy configuration for a project.
 * Encapsulates the MFA settings and provides policy checking logic.
 */
public record MfaPolicy(boolean enabled, boolean required) {

  /**
   * Creates a new MFA policy with validation.
   *
   * @param enabled whether MFA is enabled (opt-in)
   * @param required whether MFA is required (force MFA)
   * @return new MfaPolicy instance
   * @throws DomainValidationException if required is true but enabled is false
   */
  public static MfaPolicy of(boolean enabled, boolean required) {
    if (required && !enabled) {
      throw new DomainValidationException("mfa_required_but_not_enabled");
    }
    return new MfaPolicy(enabled, required);
  }

  /**
   * Creates a disabled MFA policy.
   */
  public static MfaPolicy disabled() {
    return new MfaPolicy(false, false);
  }

  /**
   * Checks if MFA is optional (enabled but not required).
   */
  public boolean isOptional() {
    return enabled && !required;
  }

  /**
   * Checks if the policy requires MFA setup for a user.
   * Returns true if MFA is required and the user is not enrolled.
   *
   * @param userMfaEnabled whether the user has MFA enabled
   * @return true if MFA setup is required
   */
  public boolean requiresMfaSetup(boolean userMfaEnabled) {
    return required && !userMfaEnabled;
  }

  /**
   * Checks if login should require MFA verification.
   * Returns true if MFA is required or if the user has MFA enabled.
   *
   * @param userMfaEnabled whether the user has MFA enabled
   * @return true if MFA verification is needed during login
   */
  public boolean requiresMfaVerification(boolean userMfaEnabled) {
    return required || userMfaEnabled;
  }
}
