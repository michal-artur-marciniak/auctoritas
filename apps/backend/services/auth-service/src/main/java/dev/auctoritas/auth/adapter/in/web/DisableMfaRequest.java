package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for MFA disable.
 *
 * @param code the TOTP code to verify before disabling MFA
 */
public record DisableMfaRequest(
    @NotBlank(message = "totp_code_required")
    @Pattern(regexp = "^\\d{6}$", message = "totp_code_invalid_format")
    String code) {
}
