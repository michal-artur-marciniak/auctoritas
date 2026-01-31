package dev.auctoritas.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auctoritas.jwt")
public record JwtProperties(
    String privateKey,
    String publicKey,
    String issuer,
    long accessTokenTtlSeconds) {

  public JwtProperties {
    if (privateKey == null || privateKey.isBlank()) {
      throw new IllegalArgumentException("JWT private key is required");
    }
    if (publicKey == null || publicKey.isBlank()) {
      throw new IllegalArgumentException("JWT public key is required");
    }
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("JWT issuer is required");
    }
    if (accessTokenTtlSeconds <= 0) {
      accessTokenTtlSeconds = 3600; // default 1 hour
    }
  }
}
