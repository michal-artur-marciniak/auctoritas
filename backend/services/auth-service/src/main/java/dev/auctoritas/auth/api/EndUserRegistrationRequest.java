package dev.auctoritas.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EndUserRegistrationRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    String name) {}
