package dev.auctoritas.auth.adapter.in.web;

import java.util.UUID;

public record EndUserRegistrationResponse(
    EndUserSummary user,
    String accessToken,
    String refreshToken) {

  public record EndUserSummary(UUID id, String email, String name, boolean emailVerified) {}
}
