package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.in.web.OrgLoginResponse;
import dev.auctoritas.auth.application.port.in.mfa.UseOrgMemberRecoveryCodeUseCase;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.application.port.out.security.JwtProviderPort;
import dev.auctoritas.auth.application.port.out.security.TokenHasherPort;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import dev.auctoritas.auth.domain.mfa.MfaChallengeRepositoryPort;
import dev.auctoritas.auth.domain.mfa.MfaRecoveryCode;
import dev.auctoritas.auth.domain.mfa.RecoveryCodeRepositoryPort;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshToken;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshTokenRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static net.logstash.logback.argument.StructuredArguments.kv;

/**
 * Service for using recovery codes to complete organization member MFA challenges.
 * Implements UC-005: UseOrgMemberRecoveryCodeUseCase from the PRD.
 */
@Service
public class UseOrgMemberRecoveryCodeService implements UseOrgMemberRecoveryCodeUseCase {

  private static final Logger log = LoggerFactory.getLogger(UseOrgMemberRecoveryCodeService.class);

  private final MfaChallengeRepositoryPort mfaChallengeRepository;
  private final RecoveryCodeRepositoryPort recoveryCodeRepository;
  private final OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public UseOrgMemberRecoveryCodeService(
      MfaChallengeRepositoryPort mfaChallengeRepository,
      RecoveryCodeRepositoryPort recoveryCodeRepository,
      OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.mfaChallengeRepository = mfaChallengeRepository;
    this.recoveryCodeRepository = recoveryCodeRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Override
  @Transactional
  public OrgLoginResponse useRecoveryCode(
      String mfaToken,
      String recoveryCode,
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

    // Validate recovery code
    String codeHash = hashRecoveryCode(recoveryCode);
    List<MfaRecoveryCode> memberCodes = recoveryCodeRepository.findByMemberId(member.getId());

    MfaRecoveryCode matchingCode = memberCodes.stream()
        .filter(code -> codeHash.equals(code.getCodeHash()) && !code.isUsed())
        .findFirst()
        .orElseThrow(() -> new DomainValidationException("recovery_code_invalid"));

    // Mark recovery code as used
    matchingCode.markUsed();
    recoveryCodeRepository.saveAll(List.of(matchingCode));

    log.info("Recovery code used for member {}", kv("memberId", member.getId()));

    // Mark challenge as used
    challenge.markUsed();
    MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

    // Publish challenge completed event
    savedChallenge.getDomainEvents().forEach(event ->
        domainEventPublisherPort.publish(event.eventType(), event));
    savedChallenge.clearDomainEvents();

    log.info("MFA challenge completed via recovery code for member {}", kv("memberId", member.getId()));

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

  private String hashRecoveryCode(String code) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
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
