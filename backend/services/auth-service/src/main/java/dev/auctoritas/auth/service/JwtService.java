package dev.auctoritas.auth.service;

import dev.auctoritas.auth.config.JwtProperties;
import dev.auctoritas.common.enums.OrgMemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  public static final String CLAIM_ORG_ID = "org_id";
  public static final String CLAIM_ORG_MEMBER_ID = "org_member_id";
  public static final String CLAIM_EMAIL = "email";
  public static final String CLAIM_ROLE = "role";

  private final JwtProperties jwtProperties;
  private final SecretKey signingKey;

  public JwtService(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
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
        .signWith(signingKey)
        .compact();
  }

  public JwtValidationResult validateToken(String token) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
      return JwtValidationResult.valid(claims);
    } catch (ExpiredJwtException e) {
      return JwtValidationResult.expired();
    } catch (JwtException e) {
      return JwtValidationResult.invalid(e.getMessage());
    }
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
