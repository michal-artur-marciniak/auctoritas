package dev.auctoritas.auth.application;
import dev.auctoritas.auth.application.apikey.ApiKeyService;

import dev.auctoritas.auth.adapter.in.web.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.adapter.in.web.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.adapter.in.web.EndUserResendVerificationRequest;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserEmailVerificationDomainService;
import dev.auctoritas.auth.domain.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import dev.auctoritas.auth.adapter.out.messaging.DomainEventPublisher;
import dev.auctoritas.auth.application.event.EmailVerificationResentEvent;
import dev.auctoritas.auth.domain.enduser.EndUserEmailVerificationTokenRepositoryPort;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
public class EndUserEmailVerificationService implements dev.auctoritas.auth.application.port.in.enduser.EndUserEmailVerificationUseCase {
  private static final Logger log = LoggerFactory.getLogger(EndUserEmailVerificationService.class);
  private static final String GENERIC_MESSAGE =
      "If an account exists, verification instructions have been sent";
  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserEmailVerificationTokenRepositoryPort verificationTokenRepository;
  private final TokenService tokenService;
  private final DomainEventPublisher domainEventPublisher;
  private final boolean logVerificationChallenge;
  private final EndUserEmailVerificationDomainService domainService;

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
    this.domainService = new EndUserEmailVerificationDomainService();
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
                  () -> new DomainValidationException("invalid_verification_token"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new DomainValidationException("invalid_verification_token");
    }

    EndUserEmailVerificationDomainService.VerificationTokenValidationResult validation =
        domainService.validateToken(
            token,
            project,
            tokenService.hashToken(rawCode),
            Instant.now());
    EndUser user = validation.user();
    user.verifyEmail();
    endUserRepository.save(user);
    publishUserDomainEvents(user);

    token.markUsed(Instant.now());
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

              EndUserEmailVerificationDomainService.ResendWindow window =
                  domainService.decideResendWindow(user.getId(), Instant.now());
              long issuedCount =
                  verificationTokenRepository.countIssuedSince(
                      user.getId(), project.getId(), window.since());
              if (issuedCount >= window.maxAllowed()) {
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
    EndUserEmailVerificationToken token = EndUserEmailVerificationToken.issue(
        user.getProject(),
        user,
        tokenService.hashToken(rawToken),
        tokenService.hashToken(rawCode),
        expiresAt);
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
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }

  private void publishUserDomainEvents(EndUser user) {
    user.getDomainEvents().forEach(event -> domainEventPublisher.publish(event.eventType(), event));
    user.clearDomainEvents();
  }
}
