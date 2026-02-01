package dev.auctoritas.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

public record OrgRefreshRequest(@NotBlank String refreshToken) {}
