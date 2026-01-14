package dev.auctoritas.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(
    @NotBlank(message = "MFA token is required")
    String mfaToken,

    @NotBlank(message = "MFA code is required")
    String code
) {}
