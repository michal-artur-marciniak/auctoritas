package dev.auctoritas.auth.application.port.out.security;

import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import io.jsonwebtoken.Claims;
import java.security.PublicKey;
import java.util.List;
import java.util.UUID;

/**
 * Port for issuing and validating JWTs for auth flows.
 */
public interface JwtProviderPort {
  String generateAccessToken(UUID orgMemberId, UUID orgId, String email, OrganizationMemberRole role);

  String generateEndUserAccessToken(
      UUID endUserId,
      UUID projectId,
      String email,
      boolean emailVerified,
      List<String> roles,
      List<String> permissions,
      long ttlSeconds);

  JwtValidationResult validateToken(String token);

  PublicKey getPublicKey();

  /**
   * Validation result for a JWT.
   */
  record JwtValidationResult(boolean valid, String errorCode, String errorMessage, Claims claims) {
    public static JwtValidationResult valid(Claims claims) {
      return new JwtValidationResult(true, null, null, claims);
    }

    public static JwtValidationResult expired() {
      return new JwtValidationResult(false, "token_expired", null, null);
    }

    public static JwtValidationResult invalid(String message) {
      return new JwtValidationResult(false, "token_invalid", message, null);
    }
  }
}
