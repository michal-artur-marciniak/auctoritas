package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.out.security.EndUserPrincipal;
import dev.auctoritas.auth.application.apikey.ApiKeyService;
import dev.auctoritas.auth.application.mfa.SetupMfaResult;
import dev.auctoritas.auth.application.port.in.mfa.SetupMfaUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.mfa.QrCodeGeneratorPort;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import dev.auctoritas.auth.domain.enduser.EndUser;
import dev.auctoritas.auth.domain.enduser.EndUserRepositoryPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.mfa.EndUserMfa;
import dev.auctoritas.auth.domain.mfa.EndUserMfaRepositoryPort;
import dev.auctoritas.auth.domain.mfa.TotpSecret;
import dev.auctoritas.auth.domain.project.ApiKey;
import dev.auctoritas.auth.domain.project.Project;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for setting up MFA for end users.
 * Implements UC-001: SetupEndUserMfaUseCase from the PRD.
 */
@Service
public class SetupEndUserMfaService implements SetupMfaUseCase {

  private static final Logger log = LoggerFactory.getLogger(SetupEndUserMfaService.class);
  private static final int RECOVERY_CODE_COUNT = 10;

  private final ApiKeyService apiKeyService;
  private final EndUserRepositoryPort endUserRepository;
  private final EndUserMfaRepositoryPort endUserMfaRepository;
  private final EncryptionPort encryptionPort;
  private final QrCodeGeneratorPort qrCodeGeneratorPort;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public SetupEndUserMfaService(
      ApiKeyService apiKeyService,
      EndUserRepositoryPort endUserRepository,
      EndUserMfaRepositoryPort endUserMfaRepository,
      EncryptionPort encryptionPort,
      QrCodeGeneratorPort qrCodeGeneratorPort,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.apiKeyService = apiKeyService;
    this.endUserRepository = endUserRepository;
    this.endUserMfaRepository = endUserMfaRepository;
    this.encryptionPort = encryptionPort;
    this.qrCodeGeneratorPort = qrCodeGeneratorPort;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public SetupMfaResult setupMfa(String apiKey, EndUserPrincipal principal) {
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

    // Check if MFA already exists for this user
    endUserMfaRepository.findByUserId(user.getId()).ifPresent(existing -> {
      throw new dev.auctoritas.auth.domain.exception.DomainConflictException("mfa_already_setup");
    });

    // Generate TOTP secret (plain for response, encrypted for storage)
    String plainSecret = encryptionPort.generateTotpSecret();
    String encryptedSecret = encryptionPort.encrypt(plainSecret);

    // Generate recovery codes
    String[] recoveryCodes = encryptionPort.generateRecoveryCodes(RECOVERY_CODE_COUNT);

    // Create EndUserMfa aggregate (not enabled until verified)
    EndUserMfa mfa = EndUserMfa.create(user, project, TotpSecret.of(encryptedSecret));

    // Persist the MFA settings
    EndUserMfa savedMfa = endUserMfaRepository.save(mfa);

    // Publish domain events
    savedMfa.getDomainEvents().forEach(event -> {
      domainEventPublisherPort.publish(event.eventType(), event);
      log.info("MFA setup initiated for user {} in project {}",
          kv("userId", user.getId()),
          kv("projectId", project.getId()));
    });
    savedMfa.clearDomainEvents();

    // Generate QR code URL
    String issuer = project.getName() != null ? project.getName() : "Auctoritas";
    String qrCodeUrl = qrCodeGeneratorPort.generateQrCodeDataUrl(plainSecret, user.getEmail(), issuer);

    log.info("MFA setup completed for user {}", kv("userId", user.getId()));

    // Return result with plain secret, QR code, and backup codes
    return new SetupMfaResult(
        plainSecret,
        qrCodeUrl,
        Arrays.asList(recoveryCodes)
    );
  }
}
