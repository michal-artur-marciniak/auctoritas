package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.in.web.OrgLoginRequest;
import dev.auctoritas.auth.adapter.in.web.OrgLoginResponse;
import dev.auctoritas.auth.adapter.in.web.OrgRefreshRequest;
import dev.auctoritas.auth.adapter.in.web.OrgRefreshResponse;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.mfa.MfaChallenge;
import dev.auctoritas.auth.domain.mfa.MfaChallengeRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberMfaRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshToken;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrgAuthService implements dev.auctoritas.auth.application.port.in.org.OrgAuthUseCase {
  private final OrganizationRepositoryPort organizationRepository;
  private final OrganizationMemberRepositoryPort organizationMemberRepository;
  private final OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository;
  private final OrganizationMemberMfaRepositoryPort orgMemberMfaRepository;
  private final MfaChallengeRepositoryPort mfaChallengeRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final TransactionTemplate transactionTemplate;

  public OrgAuthService(
      OrganizationRepositoryPort organizationRepository,
      OrganizationMemberRepositoryPort organizationMemberRepository,
      OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository,
      OrganizationMemberMfaRepositoryPort orgMemberMfaRepository,
      MfaChallengeRepositoryPort mfaChallengeRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort,
      PlatformTransactionManager transactionManager) {
    this.organizationRepository = organizationRepository;
    this.organizationMemberRepository = organizationMemberRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.orgMemberMfaRepository = orgMemberMfaRepository;
    this.mfaChallengeRepository = mfaChallengeRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public OrgLoginResponse login(OrgLoginRequest request) {
    LoginResult result = transactionTemplate.execute(status -> loginInTransaction(request));
    if (result == null) {
      throw new IllegalStateException("org_login_failed");
    }

    if (result instanceof LoginResult.MfaChallenge challenge) {
      return OrgLoginResponse.mfaChallenge(challenge.mfaToken());
    }

    LoginResult.Success success = (LoginResult.Success) result;
    String accessToken =
        jwtService.generateAccessToken(
            success.member().getId(),
            success.organization().getId(),
            success.member().getEmail(),
            success.member().getRole());

    return OrgLoginResponse.success(
        new OrgLoginResponse.OrganizationSummary(
            success.organization().getId(),
            success.organization().getName(),
            success.organization().getSlug()),
        new OrgLoginResponse.MemberSummary(
            success.member().getId(),
            success.member().getEmail(),
            success.member().getRole()),
        accessToken,
        success.rawRefreshToken());
  }

  private LoginResult loginInTransaction(OrgLoginRequest request) {
    String slug = normalizeSlug(requireValue(request.orgSlug(), "org_slug_required"));
    String email = normalizeEmail(requireValue(request.email(), "email_required"));
    String password = requireValue(request.password(), "password_required");

    Organization organization =
        organizationRepository
            .findBySlug(slug)
            .orElseThrow(
                () -> new DomainValidationException("invalid_credentials"));

    OrganizationMember member =
        organizationMemberRepository
            .findByEmailAndOrganizationId(email, organization.getId())
            .orElseThrow(
                () -> new DomainValidationException("invalid_credentials"));

    if (!passwordEncoder.matches(password, member.getPasswordHash())) {
      throw new DomainValidationException("invalid_credentials");
    }

    if (!Boolean.TRUE.equals(member.getEmailVerified())) {
      throw new DomainValidationException("email_not_verified");
    }

    // Check if MFA is enabled for the member
    boolean memberMfaEnabled = orgMemberMfaRepository.isEnabledByMemberId(member.getId());

    if (memberMfaEnabled) {
      // Create MFA challenge
      String mfaToken = tokenService.generateMfaChallengeToken();
      Instant expiresAt = tokenService.getMfaChallengeTokenExpiry();
      MfaChallenge challenge = MfaChallenge.createForMember(member, organization, mfaToken, expiresAt);
      MfaChallenge savedChallenge = mfaChallengeRepository.save(challenge);

      // Publish challenge events
      savedChallenge.getDomainEvents().forEach(event ->
          domainEventPublisherPort.publish(event.eventType(), event));
      savedChallenge.clearDomainEvents();

      return new LoginResult.MfaChallenge(mfaToken);
    }

    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(member, rawRefreshToken, null, null);

    return new LoginResult.Success(organization, member, rawRefreshToken);
  }

  private sealed interface LoginResult {
    record Success(Organization organization, OrganizationMember member, String rawRefreshToken) implements LoginResult {}
    record MfaChallenge(String mfaToken) implements LoginResult {}
  }

  public OrgRefreshResponse refresh(OrgRefreshRequest request) {
    RefreshContext context = transactionTemplate.execute(status -> refreshInTransaction(request));
    if (context == null) {
      throw new IllegalStateException("org_refresh_failed");
    }

    String accessToken =
        jwtService.generateAccessToken(
            context.memberId(),
            context.organizationId(),
            context.memberEmail(),
            context.memberRole());

    return new OrgRefreshResponse(accessToken, context.rawRefreshToken());
  }

  private RefreshContext refreshInTransaction(OrgRefreshRequest request) {
    String rawToken = requireValue(request.refreshToken(), "refresh_token_required");
    String tokenHash = tokenService.hashToken(rawToken);

    OrganizationMemberRefreshToken existingToken;
    try {
      existingToken =
          refreshTokenRepository
              .findByTokenHash(tokenHash)
              .orElseThrow(
                  () -> new DomainValidationException(
                      "invalid_refresh_token"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new DomainValidationException("invalid_refresh_token");
    }

    if (existingToken.isRevoked()) {
      throw new DomainValidationException("refresh_token_revoked");
    }

    if (existingToken.getExpiresAt().isBefore(Instant.now())) {
      throw new DomainValidationException("refresh_token_expired");
    }

    // Use rich domain model's rotate method
    String newRawRefreshToken = tokenService.generateRefreshToken();
    OrganizationMemberRefreshToken newToken = existingToken.rotate(
        tokenService.hashToken(newRawRefreshToken),
        Duration.ofHours(720), // 30 days default
        existingToken.getIpAddress(),
        existingToken.getUserAgent());

    // Save both tokens (old one is now revoked, new one is created)
    refreshTokenRepository.save(existingToken);
    refreshTokenRepository.save(newToken);

    // Publish events from both tokens
    existingToken.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    existingToken.clearDomainEvents();
    newToken.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    newToken.clearDomainEvents();

    OrganizationMember member = existingToken.getMember();
    Organization organization = member.getOrganization();

    return new RefreshContext(
        organization.getId(),
        member.getId(),
        member.getEmail(),
        member.getRole(),
        newRawRefreshToken);
  }

  private record LoginContext(
      java.util.UUID organizationId,
      String organizationName,
      String organizationSlug,
      java.util.UUID memberId,
      String memberEmail,
      OrganizationMemberRole memberRole,
      String rawRefreshToken) {}

  private record RefreshContext(
      java.util.UUID organizationId,
      java.util.UUID memberId,
      String memberEmail,
      OrganizationMemberRole memberRole,
      String rawRefreshToken) {}

  private OrganizationMemberRefreshToken persistRefreshToken(
      OrganizationMember member, String rawToken, String ipAddress, String userAgent) {
    OrganizationMemberRefreshToken token =
        OrganizationMemberRefreshToken.create(
            member,
            tokenService.hashToken(rawToken),
            Duration.ofHours(720), // 30 days default
            ipAddress,
            userAgent);
    refreshTokenRepository.save(token);

    // Publish and clear domain events
    token.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    token.clearDomainEvents();

    return token;
  }

  private String normalizeSlug(String slug) {
    return slug.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new DomainValidationException(errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new DomainValidationException(errorCode);
    }
    return trimmed;
  }
}
