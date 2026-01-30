package dev.auctoritas.auth.ports.security;

import dev.auctoritas.auth.domain.organization.OrgMemberRole;
import io.jsonwebtoken.Claims;
import java.security.PublicKey;
import java.util.UUID;

/**
 * Port for issuing and validating JWTs for auth flows.
 */
public interface JwtProviderPort {
  String generateAccessToken(UUID orgMemberId, UUID orgId, String email, OrgMemberRole role);

  String generateEndUserAccessToken(
      UUID endUserId,
      UUID projectId,
      String email,
      boolean emailVerified,
      long ttlSeconds);

  JwtValidationResult validateToken(String token);

  PublicKey getPublicKey();

  /**
   * Validation result for a JWT.
   */
  record JwtValidationResult(boolean valid, String errorCode, Claims claims) {
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
