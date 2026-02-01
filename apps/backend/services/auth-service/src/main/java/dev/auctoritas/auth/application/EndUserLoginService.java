package dev.auctoritas.auth.application;
import dev.auctoritas.auth.application.apikey.ApiKeyService;

import dev.auctoritas.auth.adapter.in.web.EndUserLoginRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserLoginResponse;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.enduser.Email;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.enduser.EndUserSession;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.domain.project.ProjectSettings;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.application.port.out.security.JwtProviderPort;
import dev.auctoritas.auth.application.port.out.security.TokenHasherPort;
import dev.auctoritas.auth.domain.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserSessionRepositoryPort;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.mfa.EndUserMfaRepositoryPort;
import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import dev.auctoritas.auth.domain.mfa.MfaChallengeRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles EndUser login and session issuance.
 * Thin application service - delegates business logic to domain entities.
 */
@Service
public class EndUserLoginService implements dev.auctoritas.auth.application.port.in.enduser.EndUserLoginUseCase {
  private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 5;
  private static final int DEFAULT_WINDOW_SECONDS = 900;
  private static final int MIN_WINDOW_SECONDS = 60;

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenHasherPort tokenHasherPort;
  private final JwtProviderPort jwtProviderPort;
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final EndUserMfaRepositoryPort endUserMfaRepository;
  private final MfaChallengeRepositoryPort mfaChallengeRepository;
  private final TransactionTemplate transactionTemplate;

  public EndUserLoginService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenHasherPort tokenHasherPort,
      JwtProviderPort jwtProviderPort,
      DomainEventPublisherPort domainEventPublisherPort,
      EndUserMfaRepositoryPort endUserMfaRepository,
      MfaChallengeRepositoryPort mfaChallengeRepository,
      PlatformTransactionManager transactionManager) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenHasherPort = tokenHasherPort;
    this.jwtProviderPort = jwtProviderPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
    this.endUserMfaRepository = endUserMfaRepository;
    this.mfaChallengeRepository = mfaChallengeRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public EndUserLoginResponse login(
      String apiKey,
      EndUserLoginRequest request,
      String ipAddress,
      String userAgent) {
    LoginResult result =
        transactionTemplate.execute(
            status -> loginInTransaction(apiKey, request, ipAddress, userAgent));
    if (result == null) {
      throw new IllegalStateException("end_user_login_failed");
    }

    if (result instanceof LoginResult.MfaChallenge challenge) {
      return EndUserLoginResponse.mfaChallenge(challenge.mfaToken());
    }

    LoginResult.Success success = (LoginResult.Success) result;
    String accessToken =
        jwtProviderPort.generateEndUserAccessToken(
            success.user().getId(),
            success.project().getId(),
            success.user().getEmail(),
            success.user().isEmailVerified(),
            success.project().getSettings().getAccessTokenTtlSeconds());

    return EndUserLoginResponse.success(
        new EndUserLoginResponse.EndUserSummary(
            success.user().getId(),
            success.user().getEmail(),
            success.user().getName(),
            success.user().isEmailVerified()),
        accessToken,
        success.rawRefreshToken());
  }

  private LoginResult loginInTransaction(
      String apiKey,
      EndUserLoginRequest request,
      String ipAddress,
      String userAgent) {

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    Email email = Email.of(request.email());
    String password = requireValue(request.password(), "password_required");

    EndUser user =
        endUserRepository
            .findByEmailAndProjectIdForUpdate(email.value(), project.getId())
            .orElseThrow(() -> new DomainValidationException("invalid_credentials"));

    Instant now = Instant.now();
    int maxAttempts = resolveMaxAttempts(settings);
    int windowSeconds = resolveWindowSeconds(settings);

    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      boolean locked = user.recordFailedLogin(maxAttempts, windowSeconds, now);
      endUserRepository.save(user);
      publishUserDomainEvents(user);
      if (locked) {
        throw new DomainValidationException("account_locked");
      }
      throw new DomainValidationException("invalid_credentials");
    }

    user.clearFailedAttempts();
    user.validateCanLogin(settings.isRequireVerifiedEmailForLogin(), now);
    endUserRepository.save(user);
    publishUserDomainEvents(user);

    // Check if MFA is enabled for this user
    boolean mfaEnabled = endUserMfaRepository.isEnabledByUserId(user.getId());
    if (mfaEnabled) {
      // Create MFA challenge
      String mfaToken = tokenHasherPort.generateMfaChallengeToken();
      Instant expiresAt = tokenHasherPort.getMfaChallengeTokenExpiry();
      MfaChallenge challenge = MfaChallenge.createForUser(user, project, mfaToken, expiresAt);
      MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

      // Publish challenge events
      savedChallenge.getDomainEvents().forEach(event ->
          domainEventPublisherPort.publish(event.eventType(), event));
      savedChallenge.clearDomainEvents();

      return new LoginResult.MfaChallenge(mfaToken);
    }

    String rawRefreshToken = createSession(user, ipAddress, userAgent);
    return new LoginResult.Success(user, project, rawRefreshToken);
  }

  private String createSession(
      EndUser user, String ipAddress, String userAgent) {

    Instant refreshExpiresAt = tokenHasherPort.getRefreshTokenExpiry();
    String rawRefreshToken = tokenHasherPort.generateRefreshToken();

    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    return rawRefreshToken;
  }

  private sealed interface LoginResult {
    record Success(EndUser user, Project project, String rawRefreshToken) implements LoginResult {}
    record MfaChallenge(String mfaToken) implements LoginResult {}
  }

  private int resolveMaxAttempts(ProjectSettings settings) {
    int configured = settings.getFailedLoginMaxAttempts();
    return configured > 0 ? configured : DEFAULT_MAX_FAILED_ATTEMPTS;
  }

  private int resolveWindowSeconds(ProjectSettings settings) {
    int configured = settings.getFailedLoginWindowSeconds();
    int resolved = configured > 0 ? configured : DEFAULT_WINDOW_SECONDS;
    return Math.max(MIN_WINDOW_SECONDS, resolved);
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
    token.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    token.clearDomainEvents();
  }

  private void persistSession(
      EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
    Duration ttl = Duration.between(Instant.now(), expiresAt);
    EndUserSession session =
        EndUserSession.create(user, trimToNull(ipAddress), buildDeviceInfo(userAgent), ttl);
    endUserSessionRepository.save(session);

    // Publish and clear domain events
    session.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    session.clearDomainEvents();
  }

  private void publishUserDomainEvents(EndUser user) {
    user.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    user.clearDomainEvents();
  }

  private Map<String, Object> buildDeviceInfo(String userAgent) {
    Map<String, Object> info = new HashMap<>();
    String resolvedAgent = trimToNull(userAgent);
    info.put("userAgent", resolvedAgent == null ? "unknown" : resolvedAgent);
    return Map.copyOf(info);
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
