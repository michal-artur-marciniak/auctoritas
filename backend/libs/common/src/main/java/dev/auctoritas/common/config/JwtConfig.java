package dev.auctoritas.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
public class JwtConfig {
  private KeyConfig keys = new KeyConfig();
  private TokenConfig token = new TokenConfig();

  @Data
  public static class KeyConfig {
    private String privateKeyPath = "keys/jwt-private.pem";
    private String publicKeyPath = "keys/jwt-public.pem";
    private int keySize = 2048;
    private boolean autoGenerate = true;
  }

  @Data
  public static class TokenConfig {
    private long accessTokenTtlSeconds = 1800; // 30 minutes
    private long refreshTokenTtlSeconds = 604800; // 7 days
    private String issuer = "auctoritas.dev";
  }
}
