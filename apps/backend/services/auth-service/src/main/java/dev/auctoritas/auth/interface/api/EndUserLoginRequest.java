package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EndUserLoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password) {}
