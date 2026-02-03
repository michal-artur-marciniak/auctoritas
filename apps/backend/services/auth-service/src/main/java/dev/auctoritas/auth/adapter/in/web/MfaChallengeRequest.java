package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for completing an MFA challenge with a TOTP code.
 *
 * @param mfaToken the MFA challenge token
 * @param code the 6-digit TOTP code
 */
public record MfaChallengeRequest(
    @NotBlank(message = "mfa_token_required")
    String mfaToken,

    @NotBlank(message = "totp_code_required")
    @Pattern(regexp = "^\\d{6}$", message = "totp_code_invalid_format")
    String code) {
}
