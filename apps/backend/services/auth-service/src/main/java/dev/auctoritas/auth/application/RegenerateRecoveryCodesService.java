package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.apikey.ApiKeyService;
import dev.auctoritas.auth.application.mfa.RegenerateRecoveryCodesResult;
import dev.auctoritas.auth.application.port.in.mfa.RegenerateRecoveryCodesUseCase;
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
import dev.auctoritas.auth.domain.mfa.RecoveryCodesRegeneratedEvent;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for regenerating recovery codes for end users.
 * Implements UC-007: RegenerateRecoveryCodesUseCase from the PRD.
 */
@Service
public class RegenerateRecoveryCodesService implements RegenerateRecoveryCodesUseCase {

  private static final Logger log = LoggerFactory.getLogger(RegenerateRecoveryCodesService.class);
  private static final int RECOVERY_CODE_COUNT = 10;

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserMfaRepositoryPort endUserMfaRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final EncryptionPort encryptionPort;
  private final TotpVerificationPort totpVerificationPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public RegenerateRecoveryCodesService(
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
  public RegenerateRecoveryCodesResult regenerateRecoveryCodes(String apiKey, EndUserPrincipal principal, String code) {
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
      log.warn("Invalid TOTP code for user {} during recovery code regeneration attempt", kv("userId", user.getId()));
      throw new DomainValidationException("totp_code_invalid");
    }

    // Delete old recovery codes
    recoveryCodeRepository.deleteByUserId(user.getId());

    // Generate new recovery codes
    String[] recoveryCodes = encryptionPort.generateRecoveryCodes(RECOVERY_CODE_COUNT);
    List<MfaRecoveryCode> recoveryCodeEntities = Arrays.stream(recoveryCodes)
        .map(codePlain -> {
          String codeHash = hashRecoveryCode(codePlain);
          return MfaRecoveryCode.createForUser(user, codeHash);
        })
        .collect(Collectors.toList());

    // Persist new recovery codes
    recoveryCodeRepository.saveAll(recoveryCodeEntities);

    // Publish domain event
    RecoveryCodesRegeneratedEvent event = new RecoveryCodesRegeneratedEvent(
        UUID.randomUUID(),
        mfa.getId(),
        user.getId(),
        project.getId(),
        recoveryCodeEntities.size(),
        Instant.now()
    );
    domainEventPublisherPort.publish(event.eventType(), event);

    log.info("Recovery codes regenerated for user {} with {} codes",
        kv("userId", user.getId()),
        kv("recoveryCodeCount", recoveryCodeEntities.size()));

    // Return result with new codes (shown once)
    return new RegenerateRecoveryCodesResult(Arrays.asList(recoveryCodes));
  }

  private String hashRecoveryCode(String code) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }
}
