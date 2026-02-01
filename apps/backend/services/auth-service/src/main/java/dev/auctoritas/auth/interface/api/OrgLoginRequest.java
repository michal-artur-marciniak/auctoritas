package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OrgLoginRequest(
    @NotBlank String orgSlug,
    @Email @NotBlank String email,
    @NotBlank String password) {}
