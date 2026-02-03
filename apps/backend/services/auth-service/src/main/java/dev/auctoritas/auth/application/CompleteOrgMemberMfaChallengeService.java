package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.in.web.OrgLoginResponse;
import dev.auctoritas.auth.application.port.in.mfa.CompleteOrgMemberMfaChallengeUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.security.EncryptionPort;
import dev.auctoritas.auth.application.port.out.security.JwtProviderPort;
import dev.auctoritas.auth.application.port.out.security.TokenHasherPort;
import dev.auctoritas.auth.application.port.out.security.TotpVerificationPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import dev.auctoritas.auth.domain.mfa.MfaChallengeRepositoryPort;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfa;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshToken;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshTokenRepositoryPort;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for completing MFA challenges during organization member login.
 * Implements UC-004: CompleteOrgMemberMfaChallengeUseCase from the PRD.
 */
@Service
public class CompleteOrgMemberMfaChallengeService implements CompleteOrgMemberMfaChallengeUseCase {

  private static final Logger log = LoggerFactory.getLogger(CompleteOrgMemberMfaChallengeService.class);

  private final MfaChallengeRepositoryPort mfaChallengeRepository;
  private final OrganizationMemberMfaRepositoryPort orgMemberMfaRepository;
  private final OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository;
  private final EncryptionPort encryptionPort;
  private final TotpVerificationPort totpVerificationPort;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public CompleteOrgMemberMfaChallengeService(
      MfaChallengeRepositoryPort mfaChallengeRepository,
      OrganizationMemberMfaRepositoryPort orgMemberMfaRepository,
      OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository,
      EncryptionPort encryptionPort,
      TotpVerificationPort totpVerificationPort,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.mfaChallengeRepository = mfaChallengeRepository;
    this.orgMemberMfaRepository = orgMemberMfaRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.encryptionPort = encryptionPort;
    this.totpVerificationPort = totpVerificationPort;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public OrgLoginResponse completeChallenge(
      String mfaToken,
      String code,
      String ipAddress,
      String userAgent) {

    // Find and validate challenge
    MfaChallenge challenge = mfaChallengeRepository
        .findByTokenForUpdate(mfaToken)
        .orElseThrow(() -> new DomainNotFoundException("mfa_challenge_not_found"));

    Instant now = Instant.now();

    // Check if challenge is still valid
    if (challenge.isExpired(now)) {
      throw new DomainValidationException("mfa_challenge_expired");
    }

    if (challenge.isUsed()) {
      throw new DomainValidationException("mfa_challenge_already_used");
    }

    // Get member from challenge
    OrganizationMember member = challenge.getMember();
    if (member == null) {
      throw new DomainValidationException("mfa_challenge_invalid");
    }

    Organization organization = member.getOrganization();

    // Get member's MFA settings
    OrganizationMemberMfa mfa = orgMemberMfaRepository
        .findByMemberIdForUpdate(member.getId())
        .orElseThrow(() -> new DomainNotFoundException("mfa_not_found"));

    // Decrypt secret and verify TOTP code
    String encryptedSecret = mfa.getSecret().encryptedValue();
    String plainSecret;
    try {
      plainSecret = encryptionPort.decrypt(encryptedSecret);
    } catch (SecurityException e) {
      log.error("Failed to decrypt TOTP secret for member {}", kv("memberId", member.getId()), e);
      throw new DomainValidationException("mfa_verification_failed");
    }

    // Verify TOTP code
    boolean validCode = totpVerificationPort.verify(plainSecret, code);
    if (!validCode) {
      throw new DomainValidationException("totp_code_invalid");
    }

    // Mark challenge as used
    challenge.markUsed();
    MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

    // Publish challenge completed event
    savedChallenge.getDomainEvents().forEach(event ->
        domainEventPublisherPort.publish(event.eventType(), event));
    savedChallenge.clearDomainEvents();

    log.info("MFA challenge completed for member {}", kv("memberId", member.getId()));

    // Create session and issue tokens
    String rawRefreshToken = createSession(member, ipAddress, userAgent);

    String accessToken = jwtService.generateAccessToken(
        member.getId(),
        organization.getId(),
        member.getEmail(),
        member.getRole());

    return OrgLoginResponse.success(
        new OrgLoginResponse.OrganizationSummary(
            organization.getId(),
            organization.getName(),
            organization.getSlug()),
        new OrgLoginResponse.MemberSummary(
            member.getId(),
            member.getEmail(),
            member.getRole()),
        accessToken,
        rawRefreshToken);
  }

  private String createSession(OrganizationMember member, String ipAddress, String userAgent) {
    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(member, rawRefreshToken, ipAddress, userAgent);
    return rawRefreshToken;
  }

  private void persistRefreshToken(
      OrganizationMember member,
      String rawToken,
      String ipAddress,
      String userAgent) {
    OrganizationMemberRefreshToken token =
        OrganizationMemberRefreshToken.create(
            member,
            tokenService.hashToken(rawToken),
            Duration.ofHours(720), // 30 days default
            trimToNull(ipAddress),
            trimToNull(userAgent));
    refreshTokenRepository.save(token);

    // Publish and clear domain events
    token.getDomainEvents().forEach(event ->
        domainEventPublisherPort.publish(event.eventType(), event));
    token.clearDomainEvents();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
