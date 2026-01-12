package dev.auctoritas.common.dto;

public record AuthTokens(
    String accessToken, String refreshToken, long expiresIn, String tokenType) {
  public AuthTokens(String accessToken, String refreshToken, long expiresIn) {
    this(accessToken, refreshToken, expiresIn, "Bearer");
  }
}
