package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.api.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.api.EndUserResendVerificationRequest;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.messaging.DomainEventPublisher;
import dev.auctoritas.auth.messaging.EmailVerificationResentEvent;
import dev.auctoritas.auth.ports.identity.EndUserEmailVerificationTokenRepositoryPort;
import dev.auctoritas.auth.ports.identity.EndUserRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class EndUserEmailVerificationService {
  private static final Logger log = LoggerFactory.getLogger(EndUserEmailVerificationService.class);
  private static final String GENERIC_MESSAGE =
      "If an account exists, verification instructions have been sent";
  private static final int RESEND_MAX_PER_HOUR = 3;
  private static final Duration RESEND_WINDOW = Duration.ofHours(1);

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserEmailVerificationTokenRepositoryPort verificationTokenRepository;
  private final TokenService tokenService;
  private final DomainEventPublisher domainEventPublisher;
  private final boolean logVerificationChallenge;

  public EndUserEmailVerificationService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserEmailVerificationTokenRepositoryPort verificationTokenRepository,
      TokenService tokenService,
      DomainEventPublisher domainEventPublisher,
      @Value("${auctoritas.auth.email-verification.log-challenge:true}") boolean logVerificationChallenge) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.tokenService = tokenService;
    this.domainEventPublisher = domainEventPublisher;
    this.logVerificationChallenge = logVerificationChallenge;
  }

  @Transactional
  public EndUserEmailVerificationResponse verifyEmail(
      String apiKey, EndUserEmailVerificationRequest request) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();

    String rawToken = requireValue(request.token(), "verification_token_required");
    String rawCode = requireValue(request.code(), "verification_code_required");

    EndUserEmailVerificationToken token;
    try {
      token =
          verificationTokenRepository
              .findByTokenHash(tokenService.hashToken(rawToken))
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_verification_token"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_verification_token");
    }

    if (!token.getProject().getId().equals(project.getId())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "api_key_invalid");
    }

    if (token.getUsedAt() != null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_token_used");
    }

    if (token.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_token_expired");
    }

    if (!tokenService.hashToken(rawCode).equals(token.getCodeHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "verification_code_invalid");
    }

    EndUser user = token.getUser();
    user.verifyEmail();
    endUserRepository.save(user);

    token.setUsedAt(Instant.now());
    verificationTokenRepository.save(token);

    return new EndUserEmailVerificationResponse("Email verified");
  }

  @Transactional
  public EndUserEmailVerificationResponse resendVerification(
      String apiKey, EndUserResendVerificationRequest request) {
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();

    String email = normalizeEmail(requireValue(request.email(), "email_required"));
    String emailHash = shortenHash(tokenService.hashToken(email));

    endUserRepository
        .findByEmailAndProjectIdForUpdate(email, project.getId())
        .ifPresentOrElse(
            user -> {
              if (user.isEmailVerified()) {
                log.info(
                    "email_verification_resend_requested {} {} {}",
                    kv("projectId", project.getId()),
                    kv("userId", user.getId()),
                    kv("outcome", "already_verified"));
                return;
              }

              Instant now = Instant.now();
              Instant since = now.minus(RESEND_WINDOW);
              long issuedCount =
                  verificationTokenRepository.countIssuedSince(
                      user.getId(), project.getId(), since);
              if (issuedCount >= RESEND_MAX_PER_HOUR) {
                log.info(
                    "email_verification_resend_requested {} {} {} {}",
                    kv("projectId", project.getId()),
                    kv("userId", user.getId()),
                    kv("outcome", "rate_limited"),
                    kv("recentIssued", issuedCount));
                return;
              }

              EmailVerificationPayload payload = issueVerificationToken(user);

              EmailVerificationResentEvent event =
                  new EmailVerificationResentEvent(
                      project.getId(),
                      user.getId(),
                      user.getEmail(),
                      payload.tokenId(),
                      payload.expiresAt());
              try {
                domainEventPublisher.publish(EmailVerificationResentEvent.EVENT_TYPE, event);
              } catch (RuntimeException ex) {
                log.warn(
                    "email_verification_resend_event_publish_failed {} {}",
                    kv("projectId", project.getId()),
                    kv("userId", user.getId()),
                    ex);
              }

              if (logVerificationChallenge) {
                log.info(
                    "Stub verification email {} {} {} {} {} {}",
                    kv("projectId", project.getId()),
                    kv("userId", user.getId()),
                    kv("email", user.getEmail()),
                    kv("verificationToken", payload.token()),
                    kv("verificationCode", payload.code()),
                    kv("expiresAt", payload.expiresAt()));
              }
            },
            () ->
                log.info(
                    "email_verification_resend_requested {} {} {}",
                    kv("projectId", project.getId()),
                    kv("emailHash", emailHash),
                    kv("outcome", "email_not_found")));

    return new EndUserEmailVerificationResponse(GENERIC_MESSAGE);
  }

  @Transactional
  public EmailVerificationPayload issueVerificationToken(EndUser user) {
    verificationTokenRepository.markUsedByUserId(user.getId(), Instant.now());

    String rawToken = tokenService.generateEmailVerificationToken();
    String rawCode = tokenService.generateEmailVerificationCode();
    Instant expiresAt = tokenService.getEmailVerificationTokenExpiry();
    EndUserEmailVerificationToken token = new EndUserEmailVerificationToken();
    token.setProject(user.getProject());
    token.setUser(user);
    token.setTokenHash(tokenService.hashToken(rawToken));
    token.setCodeHash(tokenService.hashToken(rawCode));
    token.setExpiresAt(expiresAt);
    EndUserEmailVerificationToken saved = verificationTokenRepository.save(token);
    return new EmailVerificationPayload(saved.getId(), rawToken, rawCode, expiresAt);
  }

  public record EmailVerificationPayload(
      UUID tokenId, String token, String code, Instant expiresAt) {}

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String shortenHash(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.length() <= 12 ? trimmed : trimmed.substring(0, 12);
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    return trimmed;
  }
}
