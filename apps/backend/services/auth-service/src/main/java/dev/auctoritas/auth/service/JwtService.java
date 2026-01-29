package dev.auctoritas.auth.service;

import dev.auctoritas.auth.config.JwtProperties;
import dev.auctoritas.auth.shared.enums.OrgMemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * JWT service using RS256 (RSA) algorithm for token signing and verification.
 * Uses asymmetric keys: private key for signing, public key for verification.
 */
@Service
public class JwtService {
  public static final String CLAIM_ORG_ID = "org_id";
  public static final String CLAIM_ORG_MEMBER_ID = "org_member_id";
  public static final String CLAIM_EMAIL = "email";
  public static final String CLAIM_ROLE = "role";
  public static final String CLAIM_PROJECT_ID = "project_id";
  public static final String CLAIM_END_USER_ID = "end_user_id";
  public static final String CLAIM_EMAIL_VERIFIED = "email_verified";

  private final JwtProperties jwtProperties;
  private final PrivateKey privateKey;
  private final PublicKey publicKey;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.privateKey = parsePrivateKey(jwtProperties.privateKey());
    this.publicKey = parsePublicKey(jwtProperties.publicKey());
  }

  public String generateAccessToken(
      UUID orgMemberId, UUID orgId, String email, OrgMemberRole role) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenTtlSeconds());

    return Jwts.builder()
        .subject(orgMemberId.toString())
        .claim(CLAIM_ORG_MEMBER_ID, orgMemberId.toString())
        .claim(CLAIM_ORG_ID, orgId.toString())
        .claim(CLAIM_EMAIL, email)
        .claim(CLAIM_ROLE, role.name())
        .issuer(jwtProperties.issuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  public String generateEndUserAccessToken(
      UUID endUserId, UUID projectId, String email, boolean emailVerified, long ttlSeconds) {
    long resolvedTtl = ttlSeconds > 0 ? ttlSeconds : jwtProperties.accessTokenTtlSeconds();
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(resolvedTtl);

    return Jwts.builder()
        .subject(endUserId.toString())
        .claim(CLAIM_END_USER_ID, endUserId.toString())
        .claim(CLAIM_PROJECT_ID, projectId.toString())
        .claim(CLAIM_EMAIL, email)
        .claim(CLAIM_EMAIL_VERIFIED, emailVerified)
        .issuer(jwtProperties.issuer())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  public JwtValidationResult validateToken(String token) {
    try {
      Claims claims =
          Jwts.parser()
              .requireIssuer(jwtProperties.issuer())
              .verifyWith(publicKey)
              .build()
              .parseSignedClaims(token)
              .getPayload();
      return JwtValidationResult.valid(claims);
    } catch (ExpiredJwtException e) {
      return JwtValidationResult.expired();
    } catch (JwtException e) {
      return JwtValidationResult.invalid(e.getMessage());
    }
  }

  /**
   * Returns the public key for external verification (e.g., JWKS endpoint).
   */
  public PublicKey getPublicKey() {
    return publicKey;
  }

  private PrivateKey parsePrivateKey(String pem) {
    try {
      String privateKeyPEM = normalizeKey(pem)
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(privateKeyPEM);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePrivate(spec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid RSA private key", e);
    }
  }

  private PublicKey parsePublicKey(String pem) {
    try {
      String publicKeyPEM = normalizeKey(pem)
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replaceAll("\\s", "");
      byte[] keyBytes = Base64.getDecoder().decode(publicKeyPEM);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(spec);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid RSA public key", e);
    }
  }

  /**
   * Normalize PEM key string by converting escaped newlines to actual newlines.
   * This handles keys passed via environment variables or YAML with escaped \n.
   */
  private String normalizeKey(String key) {
    return key.replace("\\n", "\n");
  }

  public record JwtValidationResult(boolean valid, String errorCode, Claims claims) {
    public static JwtValidationResult valid(Claims claims) {
      return new JwtValidationResult(true, null, claims);
    }

    public static JwtValidationResult expired() {
      return new JwtValidationResult(false, "token_expired", null);
    }

    public static JwtValidationResult invalid(String message) {
      return new JwtValidationResult(false, "token_invalid", null);
    }
  }
}
