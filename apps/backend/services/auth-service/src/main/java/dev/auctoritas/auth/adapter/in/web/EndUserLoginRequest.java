package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EndUserLoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password) {}
