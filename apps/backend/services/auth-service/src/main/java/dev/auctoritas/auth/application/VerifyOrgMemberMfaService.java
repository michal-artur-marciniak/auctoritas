package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.port.in.mfa.VerifyOrgMemberMfaUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import dev.auctoritas.auth.application.port.out.security.TotpVerificationPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.MfaRecoveryCode;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for verifying MFA and enabling it for organization members.
 * Implements VerifyOrgMemberMfaUseCase.
 */
@Service
public class VerifyOrgMemberMfaService implements VerifyOrgMemberMfaUseCase {

  private static final Logger log = LoggerFactory.getLogger(VerifyOrgMemberMfaService.class);
  private static final int RECOVERY_CODE_COUNT = 10;

  private final OrganizationMemberRepositoryPort orgMemberRepository;
  private final OrganizationMemberMfaRepositoryPort orgMemberMfaRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final EncryptionPort encryptionPort;
  private final TotpVerificationPort totpVerificationPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public VerifyOrgMemberMfaService(
      OrganizationMemberRepositoryPort orgMemberRepository,
      OrganizationMemberMfaRepositoryPort orgMemberMfaRepository,
      RecoveryCodeRepositoryPort recoveryCodeRepository,
      EncryptionPort encryptionPort,
      TotpVerificationPort totpVerificationPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.orgMemberRepository = orgMemberRepository;
    this.orgMemberMfaRepository = orgMemberMfaRepository;
    this.recoveryCodeRepository = recoveryCodeRepository;
    this.encryptionPort = encryptionPort;
    this.totpVerificationPort = totpVerificationPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public void verifyMfa(OrganizationMemberPrincipal principal, String code) {
    // Load organization member
    OrganizationMember member = orgMemberRepository.findById(principal.orgMemberId())
        .orElseThrow(() -> new DomainNotFoundException("org_member_not_found"));

    // Find MFA settings for member (must exist from setup)
    OrganizationMemberMfa mfa = orgMemberMfaRepository
        .findByMemberIdForUpdate(member.getId())
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
      log.error("Failed to decrypt TOTP secret for org member {}", kv("memberId", member.getId()), e);
      throw new DomainValidationException("mfa_verification_failed");
    }

    // Verify TOTP code
    if (!totpVerificationPort.verify(plainSecret, code)) {
      log.warn("Invalid TOTP code for org member {}", kv("memberId", member.getId()));
      throw new DomainValidationException("totp_code_invalid");
    }

    // Enable MFA
    mfa.enable();

    // Persist enabled MFA state
    OrganizationMemberMfa savedMfa = orgMemberMfaRepository.save(mfa);

    // Generate and persist recovery codes
    String[] recoveryCodes = encryptionPort.generateRecoveryCodes(RECOVERY_CODE_COUNT);
    List<MfaRecoveryCode> recoveryCodeEntities = Arrays.stream(recoveryCodes)
        .map(codePlain -> {
          String codeHash = hashRecoveryCode(codePlain);
          return MfaRecoveryCode.createForMember(member, codeHash);
        })
        .collect(Collectors.toList());

    recoveryCodeRepository.saveAll(recoveryCodeEntities);

    // Publish domain events (MfaEnabledEvent)
    savedMfa.getDomainEvents().forEach(event -> {
      domainEventPublisherPort.publish(event.eventType(), event);
      log.info("MFA enabled for org member {} in organization {}",
          kv("memberId", member.getId()),
          kv("organizationId", member.getOrganization().getId()));
    });
    savedMfa.clearDomainEvents();

    log.info("MFA verification successful for org member {} with {} recovery codes",
        kv("memberId", member.getId()),
        kv("recoveryCodeCount", recoveryCodeEntities.size()));
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
