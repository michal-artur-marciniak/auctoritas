package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrgRegistrationRequest;
import dev.auctoritas.auth.api.OrgRegistrationResponse;
import dev.auctoritas.auth.entity.organization.OrgMemberRefreshToken;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.auth.repository.OrgMemberRefreshTokenRepository;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import dev.auctoritas.auth.repository.OrganizationRepository;
import dev.auctoritas.common.enums.OrgMemberRole;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationRegistrationService {
  private final OrganizationRepository organizationRepository;
  private final OrganizationMemberRepository organizationMemberRepository;
  private final OrgMemberRefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public OrganizationRegistrationService(
      OrganizationRepository organizationRepository,
      OrganizationMemberRepository organizationMemberRepository,
      OrgMemberRefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.organizationRepository = organizationRepository;
    this.organizationMemberRepository = organizationMemberRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  @Transactional
  public OrgRegistrationResponse register(OrgRegistrationRequest request) {
    String slug = normalizeSlug(requireValue(request.slug(), "org_slug_required"));
    if (organizationRepository.existsBySlug(slug)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "org_slug_taken");
    }

    Organization organization = new Organization();
    organization.setName(requireValue(request.orgName(), "org_name_required"));
    organization.setSlug(slug);

    OrganizationMember member = new OrganizationMember();
    member.setOrganization(organization);
    member.setEmail(normalizeEmail(requireValue(request.ownerEmail(), "owner_email_required")));
    member.setPasswordHash(
        passwordEncoder.encode(requireValue(request.ownerPassword(), "owner_password_required")));
    member.setName(trimToNull(request.ownerName()));
    member.setRole(OrgMemberRole.OWNER);
    member.setEmailVerified(true);

    organization.getMembers().add(member);

    Organization savedOrganization = organizationRepository.save(organization);
    OrganizationMember savedMember = organizationMemberRepository.save(member);

    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(savedMember, rawRefreshToken);

    return new OrgRegistrationResponse(
        new OrgRegistrationResponse.OrganizationSummary(
            savedOrganization.getId(), savedOrganization.getName(), savedOrganization.getSlug()),
        new OrgRegistrationResponse.MemberSummary(
            savedMember.getId(), savedMember.getEmail(), savedMember.getRole()),
        tokenService.generateAccessToken(),
        rawRefreshToken);
  }

  private void persistRefreshToken(OrganizationMember member, String rawToken) {
    OrgMemberRefreshToken token = new OrgMemberRefreshToken();
    token.setMember(member);
    token.setTokenHash(tokenService.hashToken(rawToken));
    token.setExpiresAt(tokenService.getRefreshTokenExpiry());
    token.setRevoked(false);
    refreshTokenRepository.save(token);
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

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
