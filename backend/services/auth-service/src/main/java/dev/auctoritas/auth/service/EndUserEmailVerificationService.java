package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.EndUserEmailVerificationRequest;
import dev.auctoritas.auth.api.EndUserEmailVerificationResponse;
import dev.auctoritas.auth.api.EndUserResendVerificationRequest;
import dev.auctoritas.auth.entity.enduser.EndUser;
import dev.auctoritas.auth.entity.enduser.EndUserEmailVerificationToken;
import dev.auctoritas.auth.entity.project.ApiKey;
import dev.auctoritas.auth.entity.project.Project;
import dev.auctoritas.auth.repository.EndUserEmailVerificationTokenRepository;
import dev.auctoritas.auth.repository.EndUserRepository;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EndUserEmailVerificationService {
  private static final String GENERIC_MESSAGE =
      "If an account exists, verification instructions have been sent";

  private final ApiKeyService apiKeyService;
  private final EndUserRepository endUserRepository;
  private final EndUserEmailVerificationTokenRepository verificationTokenRepository;
  private final TokenService tokenService;

  public EndUserEmailVerificationService(
      ApiKeyService apiKeyService,
      EndUserRepository endUserRepository,
      EndUserEmailVerificationTokenRepository verificationTokenRepository,
      TokenService tokenService) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.verificationTokenRepository = verificationTokenRepository;
    this.tokenService = tokenService;
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

    if (!token.getUser().getProject().getId().equals(project.getId())) {
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
    user.setEmailVerified(true);
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

    endUserRepository
        .findByEmailAndProjectId(email, project.getId())
        .ifPresent(
            user -> {
              if (!Boolean.TRUE.equals(user.getEmailVerified())) {
                issueVerificationToken(user);
              }
            });

    return new EndUserEmailVerificationResponse(GENERIC_MESSAGE);
  }

  @Transactional
  public EmailVerificationPayload issueVerificationToken(EndUser user) {
    verificationTokenRepository.markUsedByUserId(user.getId(), Instant.now());

    String rawToken = tokenService.generateEmailVerificationToken();
    String rawCode = tokenService.generateEmailVerificationCode();
    Instant expiresAt = tokenService.getEmailVerificationTokenExpiry();
    EndUserEmailVerificationToken token = new EndUserEmailVerificationToken();
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
