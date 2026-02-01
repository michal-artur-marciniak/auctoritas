package dev.auctoritas.auth.application.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.auctoritas.auth.domain.exception.DomainExternalServiceException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AppleIdTokenValidator {
  private static final String APPLE_ISSUER = "https://appleid.apple.com";
  private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
  private static final Duration JWKS_CACHE_TTL = Duration.ofHours(6);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  private volatile CachedKeys cachedKeys = new CachedKeys(Map.of(), Instant.EPOCH);

  public AppleIdTokenValidator(RestClient.Builder builder, ObjectMapper objectMapper) {
    this.restClient = builder.build();
    this.objectMapper = objectMapper;
  }

  public AppleIdTokenClaims validate(String idToken, String expectedAudience) {
    String token = requireValue(idToken, "oauth_apple_userinfo_failed");
    String audience = requireValue(expectedAudience, "oauth_apple_userinfo_failed");

    String kid = extractKid(token);
    RSAPublicKey key = resolveKey(kid);

    Claims claims;
    try {
      claims =
          Jwts.parser()
              .requireIssuer(APPLE_ISSUER)
              .verifyWith(key)
              .build()
              .parseSignedClaims(token)
              .getPayload();
    } catch (ExpiredJwtException e) {
      throw new DomainExternalServiceException("oauth_apple_userinfo_failed", e);
    } catch (JwtException e) {
      throw new DomainExternalServiceException("oauth_apple_userinfo_failed", e);
    }

    // Apple sets aud=serviceId (client_id).
    // Depending on JJWT version, getAudience() may return a Set.
    var aud = claims.getAudience();
    if (aud == null || !aud.contains(audience)) {
      throw new DomainExternalServiceException("oauth_apple_userinfo_failed");
    }

    String providerUserId = claims.getSubject();
    String email = claims.get("email", String.class);
    Boolean emailVerified = parseEmailVerified(claims.get("email_verified"));

    return new AppleIdTokenClaims(providerUserId, email, emailVerified);
  }

  private RSAPublicKey resolveKey(String kid) {
    CachedKeys snapshot = cachedKeys;
    RSAPublicKey key = snapshot.byKid.get(kid);
    if (key != null && !snapshot.isExpired()) {
      return key;
    }

    synchronized (this) {
      snapshot = cachedKeys;
      key = snapshot.byKid.get(kid);
      if (key != null && !snapshot.isExpired()) {
        return key;
      }

      CachedKeys refreshed = fetchKeys();
      cachedKeys = refreshed;
      RSAPublicKey resolved = refreshed.byKid.get(kid);
      if (resolved == null) {
        throw new DomainExternalServiceException("oauth_apple_userinfo_failed");
      }
      return resolved;
    }
  }

  private CachedKeys fetchKeys() {
    try {
      AppleJwksResponse response =
          restClient.get().uri(APPLE_JWKS_URL).retrieve().body(AppleJwksResponse.class);
      if (response == null || response.keys() == null || response.keys().isEmpty()) {
        throw new DomainExternalServiceException("oauth_apple_userinfo_failed");
      }

      Map<String, RSAPublicKey> byKid = new HashMap<>();
      for (AppleJwk jwk : response.keys()) {
        if (jwk == null || jwk.kid() == null || jwk.kid().isBlank()) {
          continue;
        }
        RSAPublicKey key = toRsaPublicKey(jwk);
        if (key != null) {
          byKid.put(jwk.kid().trim(), key);
        }
      }
      return new CachedKeys(Map.copyOf(byKid), Instant.now().plus(JWKS_CACHE_TTL));
    } catch (RestClientException ex) {
      throw new DomainExternalServiceException("oauth_apple_userinfo_failed", ex);
    }
  }

  private RSAPublicKey toRsaPublicKey(AppleJwk jwk) {
    if (jwk == null) {
      return null;
    }
    if (jwk.n() == null || jwk.e() == null) {
      return null;
    }

    try {
      byte[] nBytes = Base64.getUrlDecoder().decode(jwk.n());
      byte[] eBytes = Base64.getUrlDecoder().decode(jwk.e());

      BigInteger modulus = new BigInteger(1, nBytes);
      BigInteger exponent = new BigInteger(1, eBytes);

      RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
      PublicKey key = KeyFactory.getInstance("RSA").generatePublic(spec);
      return (RSAPublicKey) key;
    } catch (Exception ex) {
      return null;
    }
  }

  private String extractKid(String jwt) {
    try {
      String[] parts = jwt.split("\\.");
      if (parts.length < 2) {
        throw new DomainExternalServiceException("oauth_apple_userinfo_failed");
      }
      byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
      JsonNode header = objectMapper.readTree(headerBytes);
      String kid = header == null ? null : header.path("kid").asText(null);
      return requireValue(kid, "oauth_apple_userinfo_failed");
    } catch (Exception ex) {
      throw new DomainExternalServiceException("oauth_apple_userinfo_failed", ex);
    }
  }

  private static Boolean parseEmailVerified(Object raw) {
    if (raw instanceof Boolean b) {
      return b;
    }
    if (raw instanceof String s) {
      String trimmed = s.trim();
      if (trimmed.equalsIgnoreCase("true")) {
        return true;
      }
      if (trimmed.equalsIgnoreCase("false")) {
        return false;
      }
    }
    return null;
  }

  private static String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainExternalServiceException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainExternalServiceException(errorCode);
    }
    return trimmed;
  }

  private record CachedKeys(Map<String, RSAPublicKey> byKid, Instant expiresAt) {
    boolean isExpired() {
      return expiresAt == null || expiresAt.isBefore(Instant.now());
    }
  }

  public record AppleIdTokenClaims(String providerUserId, String email, Boolean emailVerified) {}

  public record AppleJwksResponse(List<AppleJwk> keys) {}

  public record AppleJwk(
      String kty,
      String kid,
      @JsonProperty("use") String use,
      String alg,
      String n,
      String e) {}
}
