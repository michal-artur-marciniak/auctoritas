package dev.auctoritas.auth.domain.enduser;

import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.project.Project;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EndUserEmailVerificationDomainService {
  private static final int RESEND_MAX_PER_HOUR = 3;
  private static final Duration RESEND_WINDOW = Duration.ofHours(1);

  public VerificationTokenValidationResult validateToken(
      EndUserEmailVerificationToken token,
      Project project,
      String codeHash,
      Instant now) {

    if (token == null) {
      throw new DomainValidationException("invalid_verification_token");
    }
    if (project == null || project.getId() == null) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }

    if (!token.belongsToProject(project.getId())) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }
    if (token.isUsed()) {
      throw new DomainValidationException("verification_token_used");
    }
    if (token.isExpired(now)) {
      throw new DomainValidationException("verification_token_expired");
    }
    if (!token.matchesCodeHash(codeHash)) {
      throw new DomainValidationException("verification_code_invalid");
    }

    return new VerificationTokenValidationResult(token.getUser());
  }

  public ResendWindow decideResendWindow(UUID userId, Instant now) {
    if (userId == null) {
      throw new DomainValidationException("user_not_found");
    }
    Objects.requireNonNull(now, "now_required");
    return new ResendWindow(userId, now.minus(RESEND_WINDOW), RESEND_MAX_PER_HOUR);
  }

  public record VerificationTokenValidationResult(EndUser user) {}

  public record ResendWindow(UUID userId, Instant since, int maxAllowed) {}
}
