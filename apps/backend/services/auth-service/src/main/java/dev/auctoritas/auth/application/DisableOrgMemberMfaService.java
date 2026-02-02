package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.out.security.OrganizationMemberPrincipal;
import dev.auctoritas.auth.application.port.in.mfa.DisableOrgMemberMfaUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import dev.auctoritas.auth.application.port.out.security.TotpVerificationPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for disabling MFA for organization members.
 * Implements DisableOrgMemberMfaUseCase.
 */
@Service
public class DisableOrgMemberMfaService implements DisableOrgMemberMfaUseCase {

  private static final Logger log = LoggerFactory.getLogger(DisableOrgMemberMfaService.class);
  private static final String DISABLE_REASON = "user_requested";

  private final OrganizationMemberRepositoryPort orgMemberRepository;
  private final OrganizationMemberMfaRepositoryPort orgMemberMfaRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final EncryptionPort encryptionPort;
  private final TotpVerificationPort totpVerificationPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public DisableOrgMemberMfaService(
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
  public void disableMfa(OrganizationMemberPrincipal principal, String code) {
    // Load organization member
    OrganizationMember member = orgMemberRepository.findById(principal.orgMemberId())
        .orElseThrow(() -> new DomainNotFoundException("org_member_not_found"));

    // Find MFA settings for member
    OrganizationMemberMfa mfa = orgMemberMfaRepository
        .findByMemberIdForUpdate(member.getId())
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
      log.error("Failed to decrypt TOTP secret for org member {}", kv("memberId", member.getId()), e);
      throw new DomainValidationException("mfa_verification_failed");
    }

    // Verify TOTP code
    if (!totpVerificationPort.verify(plainSecret, code)) {
      log.warn("Invalid TOTP code for org member {} during MFA disable attempt", kv("memberId", member.getId()));
      throw new DomainValidationException("totp_code_invalid");
    }

    // Disable MFA
    mfa.disable(DISABLE_REASON);

    // Persist disabled MFA state
    OrganizationMemberMfa savedMfa = orgMemberMfaRepository.save(mfa);

    // Delete all recovery codes
    recoveryCodeRepository.deleteByMemberId(member.getId());

    // Publish domain events (MfaDisabledEvent)
    savedMfa.getDomainEvents().forEach(event -> {
      domainEventPublisherPort.publish(event.eventType(), event);
      log.info("MFA disabled for org member {} in organization {}",
          kv("memberId", member.getId()),
          kv("organizationId", member.getOrganization().getId()));
    });
    savedMfa.clearDomainEvents();

    log.info("MFA disabled successfully for org member {}", kv("memberId", member.getId()));
  }
}
