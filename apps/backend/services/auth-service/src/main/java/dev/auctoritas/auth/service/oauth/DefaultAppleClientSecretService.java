package dev.auctoritas.auth.service.oauth;

import dev.auctoritas.auth.domain.exception.DomainValidationException;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class DefaultAppleClientSecretService implements AppleClientSecretService {
  private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
  private static final Duration CLIENT_SECRET_TTL = Duration.ofMinutes(5);

  @Override
  public String createClientSecret(String teamId, String keyId, String serviceId, String privateKeyPem) {
    String resolvedTeamId = requireValue(teamId, "oauth_apple_not_configured");
    String resolvedKeyId = requireValue(keyId, "oauth_apple_not_configured");
    String resolvedServiceId = requireValue(serviceId, "oauth_apple_not_configured");
    String resolvedPem = requireValue(privateKeyPem, "oauth_apple_not_configured");

    PrivateKey privateKey = parseEcPrivateKey(resolvedPem);
    Instant now = Instant.now();
    Instant expiresAt = now.plus(CLIENT_SECRET_TTL);

    try {
      return Jwts.builder()
          .header()
          .keyId(resolvedKeyId)
          .and()
          .issuer(resolvedTeamId)
          .subject(resolvedServiceId)
          .audience()
          .add(APPLE_AUDIENCE)
          .and()
          .issuedAt(Date.from(now))
          .expiration(Date.from(expiresAt))
          .signWith(privateKey, Jwts.SIG.ES256)
          .compact();
    } catch (Exception ex) {
      throw new DomainValidationException("oauth_apple_not_configured", ex);
    }
  }

  private static PrivateKey parseEcPrivateKey(String pem) {
    try {
      String normalized = normalizeKey(pem);

      if (normalized.contains("-----BEGIN EC PRIVATE KEY-----")) {
        throw new DomainValidationException("oauth_apple_private_key_pkcs8_required");
      }

      String privateKeyPEM =
          normalized
              .replace("-----BEGIN PRIVATE KEY-----", "")
              .replace("-----END PRIVATE KEY-----", "")
              .replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("EC");
      return keyFactory.generatePrivate(spec);
    } catch (DomainValidationException ex) {
      throw ex;
    } catch (Exception e) {
      throw new DomainValidationException("oauth_apple_not_configured", e);
    }
  }

  private static String normalizeKey(String key) {
    return key.replace("\\n", "\n");
  }

  private static String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
