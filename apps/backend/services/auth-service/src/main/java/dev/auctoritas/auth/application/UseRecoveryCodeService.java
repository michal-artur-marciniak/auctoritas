package dev.auctoritas.auth.application;

import dev.auctoritas.auth.application.apikey.ApiKeyService;
import dev.auctoritas.auth.application.mfa.RecoveryCodeHasher;
import dev.auctoritas.auth.application.port.in.enduser.EndUserLoginResult;
import dev.auctoritas.auth.application.port.in.mfa.UseRecoveryCodeUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.security.JwtProviderPort;
import dev.auctoritas.auth.application.port.out.security.TokenHasherPort;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.enduser.EndUserSession;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import dev.auctoritas.auth.domain.mfa.MfaChallengeRepositoryPort;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for using recovery codes to complete MFA challenges.
 * Implements UC-004: UseRecoveryCodeUseCase from the PRD.
 */
@Service
public class UseRecoveryCodeService implements UseRecoveryCodeUseCase {

  private static final Logger log = LoggerFactory.getLogger(UseRecoveryCodeService.class);

  private final ApiKeyService apiKeyService;
  private final MfaChallengeRepositoryPort mfaChallengeRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository;
  private final TokenHasherPort tokenHasherPort;
  private final JwtProviderPort jwtProviderPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public UseRecoveryCodeService(
      ApiKeyService apiKeyService,
      MfaChallengeRepositoryPort mfaChallengeRepository,
      RecoveryCodeRepositoryPort recoveryCodeRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository,
      TokenHasherPort tokenHasherPort,
      JwtProviderPort jwtProviderPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyService = apiKeyService;
    this.mfaChallengeRepository = mfaChallengeRepository;
    this.recoveryCodeRepository = recoveryCodeRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.tokenHasherPort = tokenHasherPort;
    this.jwtProviderPort = jwtProviderPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public EndUserLoginResult useRecoveryCode(
      String apiKey,
      String mfaToken,
      String recoveryCode,
      String ipAddress,
      String userAgent) {

    // Validate API key and get project
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();

    // Find and validate challenge
    MfaChallenge challenge = mfaChallengeRepository
        .findByTokenForUpdate(mfaToken)
        .orElseThrow(() -> new DomainNotFoundException("mfa_challenge_not_found"));

    Instant now = Instant.now();

    // Validate challenge is for the correct project
    if (challenge.getProject() == null || !project.getId().equals(challenge.getProject().getId())) {
      throw new DomainValidationException("mfa_challenge_invalid_project");
    }

    // Check if challenge is still valid
    if (challenge.isExpired(now)) {
      throw new DomainValidationException("mfa_challenge_expired");
    }

    if (challenge.isUsed()) {
      throw new DomainValidationException("mfa_challenge_already_used");
    }

    // Get user from challenge
    EndUser user = challenge.getUser();
    if (user == null) {
      throw new DomainValidationException("mfa_challenge_invalid");
    }

    // Validate recovery code with atomic update
    String codeHash = RecoveryCodeHasher.hash(recoveryCode);
    boolean updated = recoveryCodeRepository.markUnusedCodeAsUsedForUser(user.getId(), codeHash);
    if (!updated) {
      throw new DomainValidationException("recovery_code_invalid");
    }

    log.info("Recovery code used for user {}", kv("userId", user.getId()));

    // Mark challenge as used
    challenge.markUsed();
    MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

    // Publish challenge completed event
    savedChallenge.getDomainEvents().forEach(event ->
        domainEventPublisherPort.publish(event.eventType(), event));
    savedChallenge.clearDomainEvents();

    log.info("MFA challenge completed via recovery code for user {}", kv("userId", user.getId()));

    // Create session and issue tokens
    String rawRefreshToken = createSession(user, ipAddress, userAgent, settings);

    String accessToken = jwtProviderPort.generateEndUserAccessToken(
        user.getId(),
        project.getId(),
        user.getEmail(),
        user.isEmailVerified(),
        settings.getAccessTokenTtlSeconds());

    return EndUserLoginResult.success(
        new EndUserLoginResult.EndUserSummary(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.isEmailVerified()),
        accessToken,
        rawRefreshToken);
  }

  private String createSession(
      EndUser user, String ipAddress, String userAgent, ProjectSettings settings) {

    Instant refreshExpiresAt = tokenHasherPort.getRefreshTokenExpiry();
    String rawRefreshToken = tokenHasherPort.generateRefreshToken();

    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    return rawRefreshToken;
  }

  private void persistRefreshToken(
      EndUser user,
      String rawToken,
      Instant expiresAt,
      String ipAddress,
      String userAgent) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    EndUserRefreshToken token =
        EndUserRefreshToken.create(
            user,
            tokenHasherPort.hashToken(rawToken),
            ttl,
            trimToNull(ipAddress),
            trimToNull(userAgent));
    endUserRefreshTokenRepository.save(token);

    // Publish and clear domain events
    token.getDomainEvents().forEach(event ->
        domainEventPublisherPort.publish(event.eventType(), event));
    token.clearDomainEvents();
  }

  private void persistSession(
      EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    EndUserSession session =
        EndUserSession.create(user, trimToNull(ipAddress), buildDeviceInfo(userAgent), ttl);
    endUserSessionRepository.save(session);

    // Publish and clear domain events
    session.getDomainEvents().forEach(event ->
        domainEventPublisherPort.publish(event.eventType(), event));
    session.clearDomainEvents();
  }

  private Map<String, Object> buildDeviceInfo(String userAgent) {
    Map<String, Object> info = new HashMap<>();
    String resolvedAgent = trimToNull(userAgent);
    info.put("userAgent", resolvedAgent == null ? "unknown" : resolvedAgent);
    return Map.copyOf(info);
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
