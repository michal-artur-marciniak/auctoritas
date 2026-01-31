package dev.auctoritas.auth.service;

import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserSession;
import dev.auctoritas.auth.domain.model.enduser.service.EndUserRegistrationDomainService;
import dev.auctoritas.auth.domain.model.enduser.service.RegistrationAttempt;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Password;
import dev.auctoritas.auth.messaging.UserRegisteredEvent;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.ports.security.JwtProviderPort;
import dev.auctoritas.auth.ports.security.TokenHasherPort;
import dev.auctoritas.auth.ports.identity.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.ports.identity.EndUserSessionRepositoryPort;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Handles EndUser registration and initial session issuance.
 * Thin application service - delegates business logic to domain entities.
 */
@Service
public class EndUserRegistrationService {
  private static final Logger log = LoggerFactory.getLogger(EndUserRegistrationService.class);

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserSessionRepositoryPort endUserSessionRepository;
  private final EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenHasherPort tokenHasherPort;
  private final JwtProviderPort jwtProviderPort;
  private final EndUserEmailVerificationService endUserEmailVerificationService;
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final EndUserRegistrationDomainService registrationDomainService;
  private final boolean logVerificationChallenge;

  public EndUserRegistrationService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserSessionRepositoryPort endUserSessionRepository,
      EndUserRefreshTokenRepositoryPort endUserRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenHasherPort tokenHasherPort,
      JwtProviderPort jwtProviderPort,
      EndUserEmailVerificationService endUserEmailVerificationService,
      DomainEventPublisherPort domainEventPublisherPort,
      EndUserRegistrationDomainService registrationDomainService,
      @Value("${auctoritas.auth.email-verification.log-challenge:true}") boolean logVerificationChallenge) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenHasherPort = tokenHasherPort;
    this.jwtProviderPort = jwtProviderPort;
    this.endUserEmailVerificationService = endUserEmailVerificationService;
    this.domainEventPublisherPort = domainEventPublisherPort;
    this.registrationDomainService = registrationDomainService;
    this.logVerificationChallenge = logVerificationChallenge;
  }

  @Transactional
  public EndUserRegistrationResult register(
      String apiKey,
      EndUserRegistrationCommand command,
      String ipAddress,
      String userAgent) {
    return register(
        apiKey,
        command.email(),
        command.password(),
        command.name(),
        ipAddress,
        userAgent);
  }

  private EndUserRegistrationResult register(
      String apiKey,
      String email,
      String password,
      String name,
      String ipAddress,
      String userAgent) {

    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();
    ProjectSettings settings = project.getSettings();

    Email validatedEmail = Email.of(email);

    if (endUserRepository.existsByEmailAndProjectId(validatedEmail.value(), project.getId())) {
      throw new DomainConflictException("email_taken");
    }

    // Delegate to domain service for business logic validation
    RegistrationAttempt attempt = registrationDomainService.prepareRegistration(
        project, settings, validatedEmail, password, name);

    // Application layer handles infrastructure concerns (password hashing)
    String hashedPassword = passwordEncoder.encode(attempt.validatedPassword().value());

    EndUser user = EndUser.create(
        attempt.project(),
        attempt.email(),
        Password.fromHash(hashedPassword),
        attempt.name());

    EndUser savedUser = endUserRepository.save(user);

    EndUserEmailVerificationService.EmailVerificationPayload verificationPayload =
        endUserEmailVerificationService.issueVerificationToken(savedUser);

    log.info(
        "user_registered {} {}",
        kv("projectId", project.getId()),
        kv("userId", savedUser.getId()));

    publishUserRegisteredEvent(savedUser, project, verificationPayload);

    if (logVerificationChallenge) {
      log.info(
          "Stub verification email {} {} {} {} {} {}",
          kv("projectId", project.getId()),
          kv("userId", savedUser.getId()),
          kv("email", savedUser.getEmail()),
          kv("verificationToken", verificationPayload.token()),
          kv("verificationCode", verificationPayload.code()),
          kv("expiresAt", verificationPayload.expiresAt()));
    }

    return createInitialSession(savedUser, project, settings, ipAddress, userAgent);
  }

  private void publishUserRegisteredEvent(
      EndUser user,
      Project project,
      EndUserEmailVerificationService.EmailVerificationPayload verificationPayload) {

    UserRegisteredEvent event =
        new UserRegisteredEvent(
            project.getId(),
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.isEmailVerified(),
            verificationPayload.tokenId(),
            verificationPayload.expiresAt());

    try {
      domainEventPublisherPort.publish(UserRegisteredEvent.EVENT_TYPE, event);
    } catch (RuntimeException ex) {
      log.warn(
          "user_registered_event_publish_failed {} {}",
          kv("projectId", project.getId()),
          kv("userId", user.getId()),
          ex);
    }
  }

  private EndUserRegistrationResult createInitialSession(
      EndUser user, Project project, ProjectSettings settings, String ipAddress, String userAgent) {

    Instant refreshExpiresAt = tokenHasherPort.getRefreshTokenExpiry();
    String rawRefreshToken = tokenHasherPort.generateRefreshToken();

    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtProviderPort.generateEndUserAccessToken(
            user.getId(),
            project.getId(),
            user.getEmail(),
            user.isEmailVerified(),
            settings.getAccessTokenTtlSeconds());

    return new EndUserRegistrationResult(
        new EndUserRegistrationResult.EndUserSummary(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.isEmailVerified()),
        accessToken,
        rawRefreshToken);
  }

  private void persistRefreshToken(
      EndUser user,
      String rawToken,
      Instant expiresAt,
      String ipAddress,
      String userAgent) {
    EndUserRefreshToken token = new EndUserRefreshToken();
    token.setUser(user);
    token.setTokenHash(tokenHasherPort.hashToken(rawToken));
    token.setExpiresAt(expiresAt);
    token.setRevoked(false);
    token.setIpAddress(trimToNull(ipAddress));
    token.setUserAgent(trimToNull(userAgent));
    endUserRefreshTokenRepository.save(token);
  }

  private void persistSession(
      EndUser user, Instant expiresAt, String ipAddress, String userAgent) {
    EndUserSession session = new EndUserSession();
    session.setUser(user);
    session.setDeviceInfo(buildDeviceInfo(userAgent));
    session.setIpAddress(trimToNull(ipAddress));
    session.setExpiresAt(expiresAt);
    endUserSessionRepository.save(session);
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
