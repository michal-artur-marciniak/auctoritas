package dev.auctoritas.auth.application;

import dev.auctoritas.auth.application.apikey.ApiKeyService;
import dev.auctoritas.auth.application.port.in.mfa.EndUserMfaPrincipal;
import dev.auctoritas.auth.application.port.in.mfa.VerifyMfaUseCase;
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
import dev.auctoritas.auth.domain.mfa.MfaRecoveryCode;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for verifying MFA and enabling it for end users.
 * Implements UC-002: VerifyEndUserMfaUseCase from the PRD.
 */
@Service
public class VerifyEndUserMfaService implements VerifyMfaUseCase {

  private static final Logger log = LoggerFactory.getLogger(VerifyEndUserMfaService.class);
  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserMfaRepositoryPort endUserMfaRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final EncryptionPort encryptionPort;
  private final TotpVerificationPort totpVerificationPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public VerifyEndUserMfaService(
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
  public void verifyMfa(String apiKey, EndUserMfaPrincipal principal, String code) {
    if (code == null || code.isBlank()) {
      throw new DomainValidationException("totp_code_invalid");
    }
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

    // Find MFA settings for user (must exist from setup)
    EndUserMfa mfa = endUserMfaRepository
        .findByUserIdForUpdate(user.getId())
        .orElseThrow(() -> new DomainValidationException("mfa_not_setup"));

    // Check if MFA is already enabled
    if (mfa.isEnabled()) {
      throw new DomainValidationException("mfa_already_enabled");
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
      log.warn("Invalid TOTP code for user {}", kv("userId", user.getId()));
      throw new DomainValidationException("totp_code_invalid");
    }

    // Enable MFA
    mfa.enable();

    // Persist enabled MFA state
    EndUserMfa savedMfa = endUserMfaRepository.save(mfa);

    // Load recovery codes generated during setup
    List<MfaRecoveryCode> recoveryCodeEntities = recoveryCodeRepository.findByUserId(user.getId());
    if (recoveryCodeEntities.isEmpty()) {
      throw new DomainValidationException("recovery_codes_missing");
    }
    boolean hasUnusedRecoveryCodes = recoveryCodeEntities.stream().anyMatch(recoveryCode -> !recoveryCode.isUsed());
    if (!hasUnusedRecoveryCodes) {
      throw new DomainValidationException("recovery_codes_missing");
    }

    // Publish domain events (MfaEnabledEvent)
    savedMfa.getDomainEvents().forEach(event -> {
      domainEventPublisherPort.publish(event.eventType(), event);
    });
    log.info("MFA enabled for user {} in project {}",
        kv("userId", user.getId()),
        kv("projectId", project.getId()));
    savedMfa.clearDomainEvents();

    log.info("MFA verification successful for user {} with {} recovery codes",
        kv("userId", user.getId()),
        kv("recoveryCodeCount", recoveryCodeEntities.size()));
  }

}
