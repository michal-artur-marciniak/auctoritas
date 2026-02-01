package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EndUserPasswordForgotRequest(@Email @NotBlank String email) {}
