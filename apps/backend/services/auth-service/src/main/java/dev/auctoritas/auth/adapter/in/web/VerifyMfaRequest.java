package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for MFA verification.
 *
 * @param code the 6-digit TOTP code
 */
public record VerifyMfaRequest(
    @NotBlank(message = "totp_code_required")
    @Pattern(regexp = "^\\d{6}$", message = "totp_code_invalid_format")
    String code) {
}
