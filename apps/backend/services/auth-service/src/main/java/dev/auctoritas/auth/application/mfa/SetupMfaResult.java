package dev.auctoritas.auth.application.mfa;

import java.util.List;

/**
 * Result payload for MFA setup.
 * Contains the TOTP secret, QR code URL, and optional recovery codes.
 * Recovery codes are shown only once; org member setup returns them on verification.
 *
 * @param secret the plain TOTP secret (Base32 encoded)
 * @param qrCodeUrl the QR code URL for authenticator apps
 * @param backupCodes the recovery codes (shown once)
 */
public record SetupMfaResult(
    String secret,
    String qrCodeUrl,
    List<String> backupCodes) {
}
