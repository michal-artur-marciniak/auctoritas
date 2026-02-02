package dev.auctoritas.auth.application;

import dev.auctoritas.auth.application.mfa.SetupMfaResult;
import dev.auctoritas.auth.application.port.in.ApplicationPrincipal;
import dev.auctoritas.auth.application.port.in.mfa.SetupOrgMemberMfaUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.mfa.QrCodeGeneratorPort;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.mfa.TotpSecret;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRepositoryPort;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for setting up MFA for organization members.
 * Implements SetupOrgMemberMfaUseCase.
 */
@Service
public class SetupOrgMemberMfaService implements SetupOrgMemberMfaUseCase {

  private static final Logger log = LoggerFactory.getLogger(SetupOrgMemberMfaService.class);
  private final OrganizationMemberRepositoryPort orgMemberRepository;
  private final OrganizationMemberMfaRepositoryPort orgMemberMfaRepository;
  private final EncryptionPort encryptionPort;
  private final QrCodeGeneratorPort qrCodeGeneratorPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public SetupOrgMemberMfaService(
      OrganizationMemberRepositoryPort orgMemberRepository,
      OrganizationMemberMfaRepositoryPort orgMemberMfaRepository,
      EncryptionPort encryptionPort,
      QrCodeGeneratorPort qrCodeGeneratorPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.orgMemberRepository = orgMemberRepository;
    this.orgMemberMfaRepository = orgMemberMfaRepository;
    this.encryptionPort = encryptionPort;
    this.qrCodeGeneratorPort = qrCodeGeneratorPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public SetupMfaResult setupMfa(ApplicationPrincipal principal) {
    // Load organization member
    OrganizationMember member = orgMemberRepository.findById(principal.memberId())
        .orElseThrow(() -> new DomainNotFoundException("org_member_not_found"));

    Organization organization = member.getOrganization();
    if (organization == null) {
      throw new DomainNotFoundException("organization_not_found");
    }

    // Check if MFA already exists for this member
    orgMemberMfaRepository.findByMemberId(member.getId()).ifPresent(existing -> {
      throw new DomainConflictException("mfa_already_setup");
    });

    // Generate TOTP secret (plain for response, encrypted for storage)
    String plainSecret = encryptionPort.generateTotpSecret();
    String encryptedSecret = encryptionPort.encrypt(plainSecret);

    // Create OrganizationMemberMfa aggregate (not enabled until verified)
    OrganizationMemberMfa mfa = OrganizationMemberMfa.create(member, organization, TotpSecret.of(encryptedSecret));

    // Persist the MFA settings
    OrganizationMemberMfa savedMfa = orgMemberMfaRepository.save(mfa);

    // Publish domain events
    savedMfa.getDomainEvents().forEach(event -> {
      domainEventPublisherPort.publish(event.eventType(), event);
      log.info("MFA setup initiated for org member {} in organization {}",
          kv("memberId", member.getId()),
          kv("organizationId", organization.getId()));
    });
    savedMfa.clearDomainEvents();

    // Generate QR code URL
    String issuer = organization.getName() != null ? organization.getName() : "Auctoritas";
    String qrCodeUrl = qrCodeGeneratorPort.generateQrCodeDataUrl(plainSecret, member.getEmail(), issuer);

    log.info("MFA setup completed for org member {}", kv("memberId", member.getId()));

    // Return result with plain secret, QR code, and backup codes
    return new SetupMfaResult(
        plainSecret,
        qrCodeUrl,
        List.of()
    );
  }

}
