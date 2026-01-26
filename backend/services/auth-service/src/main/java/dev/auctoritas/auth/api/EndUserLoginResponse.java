package dev.auctoritas.auth.api;

import java.util.UUID;

public record EndUserLoginResponse(
    EndUserSummary user,
    String accessToken,
    String refreshToken) {

  public record EndUserSummary(UUID id, String email, String name, boolean emailVerified) {}
}
