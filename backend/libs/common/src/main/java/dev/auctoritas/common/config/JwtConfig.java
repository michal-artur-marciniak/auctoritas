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
  }

  @Data
  public static class TokenConfig {
    private long accessTokenExpirationSeconds = 1800;
    private long refreshTokenExpirationSeconds = 2592000;
    private String issuer = "auctoritas";
  }
}
