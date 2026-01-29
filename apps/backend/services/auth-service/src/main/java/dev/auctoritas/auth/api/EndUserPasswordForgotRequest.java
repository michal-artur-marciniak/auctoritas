package dev.auctoritas.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordForgotRequest(@Email @NotBlank String email) {}
