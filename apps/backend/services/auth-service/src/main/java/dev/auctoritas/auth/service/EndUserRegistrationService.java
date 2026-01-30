package dev.auctoritas.auth.service;

import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserRefreshToken;
import dev.auctoritas.auth.entity.enduser.EndUserSession;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.entity.project.ProjectSettings;
import dev.auctoritas.auth.messaging.UserRegisteredEvent;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.ports.security.JwtProviderPort;
import dev.auctoritas.auth.ports.security.TokenHasherPort;
import dev.auctoritas.auth.repository.EndUserRefreshTokenRepository;
import dev.auctoritas.auth.repository.EndUserSessionRepository;
import dev.auctoritas.auth.shared.security.PasswordPolicy;
import dev.auctoritas.auth.shared.security.PasswordValidator;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
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
 */
@Service
public class EndUserRegistrationService {
  private static final Logger log = LoggerFactory.getLogger(EndUserRegistrationService.class);
  private static final int DEFAULT_MAX_PASSWORD_LENGTH = 128;
  private static final int DEFAULT_MIN_UNIQUE = 4;

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserSessionRepository endUserSessionRepository;
  private final EndUserRefreshTokenRepository endUserRefreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenHasherPort tokenHasherPort;
  private final JwtProviderPort jwtProviderPort;
  private final EndUserEmailVerificationService endUserEmailVerificationService;
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final boolean logVerificationChallenge;

  public EndUserRegistrationService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserSessionRepository endUserSessionRepository,
      EndUserRefreshTokenRepository endUserRefreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenHasherPort tokenHasherPort,
      JwtProviderPort jwtProviderPort,
      EndUserEmailVerificationService endUserEmailVerificationService,
      DomainEventPublisherPort domainEventPublisherPort,
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
    if (settings == null) {
      throw new DomainValidationException("project_settings_missing");
    }

    String normalizedEmail = normalizeEmail(requireValue(email, "email_required"));
    String normalizedPassword = requireValue(password, "password_required");

    if (endUserRepository.existsByEmailAndProjectId(normalizedEmail, project.getId())) {
      throw new DomainConflictException("email_taken");
    }

    validatePassword(settings, normalizedPassword);

    EndUser user = new EndUser();
    user.setProject(project);
    user.setEmail(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(normalizedPassword));
    user.setName(trimToNull(name));

    EndUser savedUser = endUserRepository.save(user);
    EndUserEmailVerificationService.EmailVerificationPayload verificationPayload =
        endUserEmailVerificationService.issueVerificationToken(savedUser);

    log.info(
        "user_registered {} {}",
        kv("projectId", project.getId()),
        kv("userId", savedUser.getId()));

    UserRegisteredEvent event =
        new UserRegisteredEvent(
            project.getId(),
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getName(),
            Boolean.TRUE.equals(savedUser.getEmailVerified()),
            verificationPayload.tokenId(),
            verificationPayload.expiresAt());
    try {
      domainEventPublisherPort.publish(UserRegisteredEvent.EVENT_TYPE, event);
    } catch (RuntimeException ex) {
      log.warn(
          "user_registered_event_publish_failed {} {}",
          kv("projectId", project.getId()),
          kv("userId", savedUser.getId()),
          ex);
    }

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

    Instant refreshExpiresAt = tokenHasherPort.getRefreshTokenExpiry();
    String rawRefreshToken = tokenHasherPort.generateRefreshToken();
    persistRefreshToken(savedUser, rawRefreshToken, refreshExpiresAt, ipAddress, userAgent);
    persistSession(savedUser, refreshExpiresAt, ipAddress, userAgent);

    String accessToken =
        jwtProviderPort.generateEndUserAccessToken(
            savedUser.getId(),
            project.getId(),
            savedUser.getEmail(),
            Boolean.TRUE.equals(savedUser.getEmailVerified()),
            settings.getAccessTokenTtlSeconds());

    return new EndUserRegistrationResult(
        new EndUserRegistrationResult.EndUserSummary(
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getName(),
            Boolean.TRUE.equals(savedUser.getEmailVerified())),
        accessToken,
        rawRefreshToken);
  }

  private void validatePassword(ProjectSettings settings, String password) {
    int minLength = settings.getMinLength();
    int minUnique = Math.max(1, Math.min(DEFAULT_MIN_UNIQUE, minLength));
    PasswordPolicy policy =
        new PasswordPolicy(
            minLength,
            DEFAULT_MAX_PASSWORD_LENGTH,
            settings.isRequireUppercase(),
            settings.isRequireLowercase(),
            settings.isRequireNumbers(),
            settings.isRequireSpecialChars(),
            minUnique);
    PasswordValidator.ValidationResult result = new PasswordValidator(policy).validate(password);
    if (!result.valid()) {
      throw new DomainValidationException("password_policy_failed");
    }
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

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
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
