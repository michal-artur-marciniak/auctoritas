package dev.auctoritas.auth.application.port.in.mfa;

import dev.auctoritas.auth.domain.project.MfaPolicy;
import java.util.UUID;

/**
 * Use case for checking MFA policy for a user.
 * Implements UC-008 from PRD.
 */
public interface CheckMfaPolicyUseCase {

  /**
   * Checks the MFA policy for a user in a project.
   * Determines if MFA is required, optional, or disabled.
   *
   * @param projectId the project ID
   * @param userId the user ID
   * @return MFA policy check result
   */
  MfaPolicyCheckResult checkMfaPolicy(UUID projectId, UUID userId);

  /**
   * Result of MFA policy check.
   *
   * @param policy the MFA policy configuration
   * @param userMfaEnabled whether the user has MFA enabled
   * @param mfaRequired whether MFA is required for this user
   * @param mfaSetupRequired whether the user needs to set up MFA
   */
  record MfaPolicyCheckResult(
      MfaPolicy policy,
      boolean userMfaEnabled,
      boolean mfaRequired,
      boolean mfaSetupRequired) {

    /**
     * Creates a result indicating MFA setup is required.
     */
    public static MfaPolicyCheckResult setupRequired(MfaPolicy policy) {
      return new MfaPolicyCheckResult(policy, false, true, true);
    }

    /**
     * Creates a result for a user with MFA enabled.
     */
    public static MfaPolicyCheckResult mfaEnabled(MfaPolicy policy) {
      return new MfaPolicyCheckResult(policy, true, true, false);
    }

    /**
     * Creates a result for MFA optional but user not enrolled.
     */
    public static MfaPolicyCheckResult mfaOptional(MfaPolicy policy) {
      return new MfaPolicyCheckResult(policy, false, false, false);
    }

    /**
     * Creates a result for MFA disabled.
     */
    public static MfaPolicyCheckResult mfaDisabled() {
      MfaPolicy policy = MfaPolicy.disabled();
      return new MfaPolicyCheckResult(policy, false, false, false);
    }
  }
}
