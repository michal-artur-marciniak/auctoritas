package dev.auctoritas.auth.interface.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrgRegistrationRequest(
    @NotBlank String orgName,
    @NotBlank String slug,
    @Email @NotBlank String ownerEmail,
    @NotBlank @Size(min = 12, max = 128) String ownerPassword,
    @NotBlank String ownerName) {}
