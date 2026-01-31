package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrgLoginRequest;
import dev.auctoritas.auth.api.OrgLoginResponse;
import dev.auctoritas.auth.api.OrgRefreshRequest;
import dev.auctoritas.auth.api.OrgRefreshResponse;
import dev.auctoritas.auth.domain.model.organization.OrgMemberRefreshToken;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.organization.OrganizationMember;
import dev.auctoritas.auth.ports.organization.OrgMemberRefreshTokenRepositoryPort;
import dev.auctoritas.auth.ports.organization.OrganizationMemberRepositoryPort;
import dev.auctoritas.auth.ports.organization.OrganizationRepositoryPort;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrgAuthService {
  private final OrganizationRepositoryPort organizationRepository;
  private final OrganizationMemberRepositoryPort organizationMemberRepository;
  private final OrgMemberRefreshTokenRepositoryPort refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtService jwtService;

  public OrgAuthService(
      OrganizationRepositoryPort organizationRepository,
      OrganizationMemberRepositoryPort organizationMemberRepository,
      OrgMemberRefreshTokenRepositoryPort refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtService jwtService) {
    this.organizationRepository = organizationRepository;
    this.organizationMemberRepository = organizationMemberRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
  }

  @Transactional
  public OrgLoginResponse login(OrgLoginRequest request) {
    String slug = normalizeSlug(requireValue(request.orgSlug(), "org_slug_required"));
    String email = normalizeEmail(requireValue(request.email(), "email_required"));
    String password = requireValue(request.password(), "password_required");

    Organization organization =
        organizationRepository
            .findBySlug(slug)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_credentials"));

    OrganizationMember member =
        organizationMemberRepository
            .findByEmailAndOrganizationId(email, organization.getId())
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_credentials"));

    if (!passwordEncoder.matches(password, member.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_credentials");
    }

    if (!Boolean.TRUE.equals(member.getEmailVerified())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email_not_verified");
    }

    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(member, rawRefreshToken, null, null);

    String accessToken =
        jwtService.generateAccessToken(
            member.getId(), organization.getId(), member.getEmail(), member.getRole());

    return new OrgLoginResponse(
        new OrgLoginResponse.OrganizationSummary(
            organization.getId(), organization.getName(), organization.getSlug()),
        new OrgLoginResponse.MemberSummary(member.getId(), member.getEmail(), member.getRole()),
        accessToken,
        rawRefreshToken);
  }

  @Transactional
  public OrgRefreshResponse refresh(OrgRefreshRequest request) {
    String rawToken = requireValue(request.refreshToken(), "refresh_token_required");
    String tokenHash = tokenService.hashToken(rawToken);

    OrgMemberRefreshToken existingToken;
    try {
      existingToken =
          refreshTokenRepository
              .findByTokenHash(tokenHash)
              .orElseThrow(
                  () -> new ResponseStatusException(
                      HttpStatus.BAD_REQUEST, "invalid_refresh_token"));
    } catch (PessimisticLockException | LockTimeoutException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid_refresh_token");
    }

    if (existingToken.isRevoked()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token_revoked");
    }

    if (existingToken.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token_expired");
    }

    // Rotate: revoke old token and issue new one
    existingToken.setRevoked(true);

    String newRawRefreshToken = tokenService.generateRefreshToken();
    OrgMemberRefreshToken newToken =
        persistRefreshToken(
            existingToken.getMember(),
            newRawRefreshToken,
            existingToken.getIpAddress(),
            existingToken.getUserAgent());

    existingToken.setReplacedBy(newToken);

    OrganizationMember member = existingToken.getMember();
    String accessToken =
        jwtService.generateAccessToken(
            member.getId(),
            member.getOrganization().getId(),
            member.getEmail(),
            member.getRole());

    return new OrgRefreshResponse(accessToken, newRawRefreshToken);
  }

  private OrgMemberRefreshToken persistRefreshToken(
      OrganizationMember member, String rawToken, String ipAddress, String userAgent) {
    OrgMemberRefreshToken token = new OrgMemberRefreshToken();
    token.setMember(member);
    token.setTokenHash(tokenService.hashToken(rawToken));
    token.setExpiresAt(tokenService.getRefreshTokenExpiry());
    token.setRevoked(false);
    token.setIpAddress(ipAddress);
    token.setUserAgent(userAgent);
    return refreshTokenRepository.save(token);
  }

  private String normalizeSlug(String slug) {
    return slug.trim().toLowerCase(Locale.ROOT);
  }

  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private String requireValue(String value, String errorCode) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorCode);
    }
    return trimmed;
  }
}
