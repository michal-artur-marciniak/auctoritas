package dev.auctoritas.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auctoritas.jwt")
public record JwtProperties(String secret, String issuer, long accessTokenTtlSeconds) {
  public JwtProperties {
    if (secret == null || secret.length() < 32) {
      throw new IllegalArgumentException("JWT secret must be at least 32 characters");
    }
    if (issuer == null || issuer.isBlank()) {
      throw new IllegalArgumentException("JWT issuer is required");
    }
    if (accessTokenTtlSeconds <= 0) {
      accessTokenTtlSeconds = 3600; // default 1 hour
    }
  }
}
