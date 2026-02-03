package dev.auctoritas.auth.application.mfa;

import java.util.List;

/**
 * Result payload for recovery code regeneration.
 * Contains the new recovery codes.
 * Recovery codes are shown only once during regeneration.
 *
 * @param backupCodes the new recovery codes (shown once)
 */
public record RegenerateRecoveryCodesResult(
    List<String> backupCodes) {
}
