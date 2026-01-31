package dev.auctoritas.auth.service;

import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.model.enduser.EndUser;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.domain.model.enduser.EndUserSession;
import dev.auctoritas.auth.domain.model.enduser.EndUserRegistrationDomainService;
import dev.auctoritas.auth.domain.model.enduser.RegistrationAttempt;
import dev.auctoritas.auth.domain.model.project.ApiKey;
import dev.auctoritas.auth.domain.model.project.Project;
import dev.auctoritas.auth.domain.model.project.ProjectSettings;
import dev.auctoritas.auth.domain.model.enduser.Email;
import dev.auctoritas.auth.domain.model.enduser.Password;
import dev.auctoritas.auth.messaging.UserRegisteredEvent;
import dev.auctoritas.auth.domain.model.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.ports.security.JwtProviderPort;
import dev.auctoritas.auth.ports.security.TokenHasherPort;
import dev.auctoritas.auth.domain.model.enduser.EndUserRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.model.enduser.EndUserSessionRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
  private final TransactionTemplate transactionTemplate;

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
      @Value("${auctoritas.auth.email-verification.log-challenge:true}") boolean logVerificationChallenge,
      PlatformTransactionManager transactionManager) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserSessionRepository = endUserSessionRepository;
    this.endUserRefreshTokenRepository = endUserRefreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenHasherPort = tokenHasherPort;
    this.jwtProviderPort = jwtProviderPort;
    this.endUserEmailVerificationService = endUserEmailVerificationService;
    this.domainEventPublisherPort = domainEventPublisherPort;
    this.registrationDomainService = new EndUserRegistrationDomainService();
    this.logVerificationChallenge = logVerificationChallenge;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

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
    RegistrationContext context =
        transactionTemplate.execute(
            status -> registerInTransaction(apiKey, email, password, name, ipAddress, userAgent));
    if (context == null) {
      throw new IllegalStateException("end_user_registration_failed");
    }

    String accessToken =
        jwtProviderPort.generateEndUserAccessToken(
            context.userId(),
            context.projectId(),
            context.email(),
            context.emailVerified(),
            context.accessTokenTtlSeconds());

    return new EndUserRegistrationResult(
        new EndUserRegistrationResult.EndUserSummary(
            context.userId(), context.email(), context.name(), context.emailVerified()),
        accessToken,
        context.rawRefreshToken());
  }

  private RegistrationContext registerInTransaction(
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

    publishUserDomainEvents(savedUser, project, verificationPayload);

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

    String rawRefreshToken = createInitialSession(savedUser, ipAddress, userAgent);

    return new RegistrationContext(
        savedUser.getId(),
        savedUser.getEmail(),
        savedUser.getName(),
        savedUser.isEmailVerified(),
        project.getId(),
        settings.getAccessTokenTtlSeconds(),
        rawRefreshToken);
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

  private void publishUserDomainEvents(
      EndUser user,
      Project project,
      EndUserEmailVerificationService.EmailVerificationPayload verificationPayload) {
    boolean userRegisteredPublished = false;
    for (var event : user.getDomainEvents()) {
      if (event instanceof dev.auctoritas.auth.domain.model.enduser.UserRegisteredEvent) {
        if (!userRegisteredPublished) {
          publishUserRegisteredEvent(user, project, verificationPayload);
          userRegisteredPublished = true;
        }
        continue;
      }
      domainEventPublisherPort.publish(event.eventType(), event);
    }
    user.clearDomainEvents();
  }

  private String createInitialSession(
      EndUser user, String ipAddress, String userAgent) {

    Instant refreshExpiresAt = tokenHasherPort.getRefreshTokenExpiry();
    String rawRefreshToken = tokenHasherPort.generateRefreshToken();

    persistRefreshToken(user, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(user, refreshExpiresAt, ipAddress, userAgent);

    return rawRefreshToken;
  }

  private record RegistrationContext(
      java.util.UUID userId,
      String email,
      String name,
      boolean emailVerified,
      java.util.UUID projectId,
      long accessTokenTtlSeconds,
      String rawRefreshToken) {}

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
