package dev.auctoritas.auth.interface.api;

import java.util.UUID;

public record EndUserRegistrationResponse(
    EndUserSummary user,
    String accessToken,
    String refreshToken) {

  public record EndUserSummary(UUID id, String email, String name, boolean emailVerified) {}
}
