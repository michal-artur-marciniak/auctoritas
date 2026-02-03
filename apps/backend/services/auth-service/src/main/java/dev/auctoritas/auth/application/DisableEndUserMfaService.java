package dev.auctoritas.auth.application;

import dev.auctoritas.auth.application.apikey.ApiKeyService;
import dev.auctoritas.auth.application.port.in.mfa.DisableMfaUseCase;
import dev.auctoritas.auth.application.port.in.mfa.EndUserMfaPrincipal;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import dev.auctoritas.auth.application.port.out.security.TotpVerificationPort;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.EndUserMfa;
import dev.auctoritas.auth.domain.mfa.EndUserMfaRepositoryPort;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for disabling MFA for end users.
 * Implements UC-006: DisableEndUserMfaUseCase from the PRD.
 */
@Service
public class DisableEndUserMfaService implements DisableMfaUseCase {

  private static final Logger log = LoggerFactory.getLogger(DisableEndUserMfaService.class);
  private static final String DISABLE_REASON = "user_requested";

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserMfaRepositoryPort endUserMfaRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final EncryptionPort encryptionPort;
  private final TotpVerificationPort totpVerificationPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public DisableEndUserMfaService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserMfaRepositoryPort endUserMfaRepository,
      RecoveryCodeRepositoryPort recoveryCodeRepository,
      EncryptionPort encryptionPort,
      TotpVerificationPort totpVerificationPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserMfaRepository = endUserMfaRepository;
    this.recoveryCodeRepository = recoveryCodeRepository;
    this.encryptionPort = encryptionPort;
    this.totpVerificationPort = totpVerificationPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public void disableMfa(String apiKey, EndUserMfaPrincipal principal, String code) {
    // Validate API key and get project
    ApiKey resolvedKey = apiKeyService.validateActiveKey(apiKey);
    Project project = resolvedKey.getProject();

    // Validate principal's project matches API key's project
    if (!project.getId().equals(principal.projectId())) {
      throw new DomainUnauthorizedException("api_key_invalid");
    }

    // Validate user exists and belongs to project
    EndUser user = endUserRepository
        .findByEmailAndProjectId(principal.email(), project.getId())
        .orElseThrow(() -> new DomainNotFoundException("user_not_found"));

    // Find MFA settings for user
    EndUserMfa mfa = endUserMfaRepository
        .findByUserIdForUpdate(user.getId())
        .orElseThrow(() -> new DomainValidationException("mfa_not_setup"));

    // Check if MFA is enabled
    if (!mfa.isEnabled()) {
      throw new DomainValidationException("mfa_not_enabled");
    }

    // Decrypt the secret for verification
    String encryptedSecret = mfa.getSecret().encryptedValue();
    String plainSecret;
    try {
      plainSecret = encryptionPort.decrypt(encryptedSecret);
    } catch (SecurityException e) {
      log.error("Failed to decrypt TOTP secret for user {}", kv("userId", user.getId()), e);
      throw new DomainValidationException("mfa_verification_failed");
    }

    // Verify TOTP code
    if (!totpVerificationPort.verify(plainSecret, code)) {
      log.warn("Invalid TOTP code for user {} during MFA disable attempt", kv("userId", user.getId()));
      throw new DomainValidationException("totp_code_invalid");
    }

    // Disable MFA
    mfa.disable(DISABLE_REASON);

    // Persist disabled MFA state
    EndUserMfa savedMfa = endUserMfaRepository.save(mfa);

    // Delete all recovery codes
    recoveryCodeRepository.deleteByUserId(user.getId());

    // Publish domain events (MfaDisabledEvent)
    savedMfa.getDomainEvents().forEach(event -> {
      domainEventPublisherPort.publish(event.eventType(), event);
      log.info("MFA disabled for user {} in project {}",
          kv("userId", user.getId()),
          kv("projectId", project.getId()));
    });
    savedMfa.clearDomainEvents();

    log.info("MFA disabled successfully for user {}", kv("userId", user.getId()));
  }
}
