package dev.auctoritas.auth.adapters.security;

import dev.auctoritas.auth.ports.security.JwtProviderPort;
import dev.auctoritas.auth.service.JwtService;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import java.security.PublicKey;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link JwtService} via {@link JwtProviderPort}.
 */
@Component
public class JwtServiceAdapter implements JwtProviderPort {
  private final JwtService jwtService;

  public JwtServiceAdapter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  public String generateAccessToken(UUID orgMemberId, UUID orgId, String email, OrganizationMemberRole role) {
    return jwtService.generateAccessToken(orgMemberId, orgId, email, role);
  }

  @Override
  public String generateEndUserAccessToken(
      UUID endUserId,
      UUID projectId,
      String email,
      boolean emailVerified,
      long ttlSeconds) {
    return jwtService.generateEndUserAccessToken(
        endUserId, projectId, email, emailVerified, ttlSeconds);
  }

  @Override
  public JwtValidationResult validateToken(String token) {
    JwtService.JwtValidationResult result = jwtService.validateToken(token);
    return new JwtValidationResult(
        result.valid(), result.errorCode(), result.errorMessage(), result.claims());
  }

  @Override
  public PublicKey getPublicKey() {
    return jwtService.getPublicKey();
  }
}
