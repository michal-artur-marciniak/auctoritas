package dev.auctoritas.auth.application.mfa;

import java.util.List;

/**
 * Result payload for MFA verification.
 * Contains recovery codes shown only once after verification.
 *
 * @param recoveryCodes the recovery codes (shown once)
 */
public record VerifyMfaResult(List<String> recoveryCodes) {
}
