package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for completing an MFA challenge with a recovery code.
 *
 * @param mfaToken the MFA challenge token
 * @param recoveryCode the recovery code
 */
public record RecoveryCodeRequest(
    @NotBlank(message = "mfa_token_required")
    String mfaToken,

    @NotBlank(message = "recovery_code_required")
    String recoveryCode) {
}