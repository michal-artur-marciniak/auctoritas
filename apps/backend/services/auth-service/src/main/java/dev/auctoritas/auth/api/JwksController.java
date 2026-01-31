package dev.auctoritas.auth.api;

import dev.auctoritas.auth.infrastructure.config.JwtProperties;
import dev.auctoritas.auth.service.JwtService;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWKS (JSON Web Key Set) endpoint for exposing public keys.
 * Follows RFC 7517 specification.
 * This endpoint allows external services to verify JWTs without sharing the private key.
 */
@RestController
@RequestMapping("/.well-known")
public class JwksController {

  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  public JwksController(JwtService jwtService, JwtProperties jwtProperties) {
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
  }

  /**
   * Returns the JSON Web Key Set containing the public key(s) used to verify JWTs.
   * Standard endpoint path: /.well-known/jwks.json
   */
  @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public JwksResponse getJwks() {
    RSAPublicKey rsaPublicKey = (RSAPublicKey) jwtService.getPublicKey();

    String kid = generateKeyId(rsaPublicKey);
    String n = base64UrlUnsigned(rsaPublicKey.getModulus());
    String e = base64UrlUnsigned(rsaPublicKey.getPublicExponent());

    Jwk jwk = new Jwk(
        "RSA",      // kty - Key Type
        "sig",      // use - Public Key Use (signature)
        "RS256",    // alg - Algorithm
        kid,        // kid - Key ID
        n,          // n - RSA modulus
        e           // e - RSA public exponent
    );

    return new JwksResponse(List.of(jwk));
  }

  /**
   * OpenID Connect discovery endpoint.
   * Returns metadata about the authorization server.
   */
  @GetMapping(value = "/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> getOpenIdConfiguration() {
    String issuer = jwtProperties.issuer();
    String jwksUri = UriComponentsBuilder.fromUriString(issuer)
        .path("/.well-known/jwks.json")
        .build()
        .toUriString();
    return Map.of(
        "issuer", issuer,
        "jwks_uri", jwksUri,
        "id_token_signing_alg_values_supported", List.of("RS256"),
        "token_endpoint_auth_methods_supported", List.of("client_secret_post", "client_secret_basic")
    );
  }

  /**
   * Generate a stable key ID from the public key.
   * Uses SHA-256 hash of the key's encoded form, truncated to first 8 bytes.
   */
  private String generateKeyId(RSAPublicKey publicKey) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(publicKey.getEncoded());
      // Use first 8 bytes of hash as key ID
      byte[] truncated = new byte[8];
      System.arraycopy(hash, 0, truncated, 0, 8);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(truncated);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is always available
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  private String base64UrlUnsigned(BigInteger value) {
    byte[] bytes = value.toByteArray();
    if (bytes.length > 1 && bytes[0] == 0) {
      byte[] unsignedBytes = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, unsignedBytes, 0, unsignedBytes.length);
      bytes = unsignedBytes;
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public record JwksResponse(List<Jwk> keys) {}

  public record Jwk(
      String kty,
      String use,
      String alg,
      String kid,
      String n,
      String e
  ) {}
}
