package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EndUserResendVerificationRequest(@Email @NotBlank String email) {}
