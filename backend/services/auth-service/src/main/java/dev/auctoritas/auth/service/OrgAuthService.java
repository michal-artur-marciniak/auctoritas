package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrgLoginRequest;
import dev.auctoritas.auth.api.OrgLoginResponse;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import dev.auctoritas.auth.repository.OrganizationRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrgAuthService {
  private final OrganizationRepository organizationRepository;
  private final OrganizationMemberRepository organizationMemberRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;

  public OrgAuthService(
      OrganizationRepository organizationRepository,
      OrganizationMemberRepository organizationMemberRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService) {
    this.organizationRepository = organizationRepository;
    this.organizationMemberRepository = organizationMemberRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
  }

  @Transactional(readOnly = true)
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

    return new OrgLoginResponse(
        new OrgLoginResponse.OrganizationSummary(
            organization.getId(), organization.getName(), organization.getSlug()),
        new OrgLoginResponse.MemberSummary(member.getId(), member.getEmail(), member.getRole()),
        tokenService.generateAccessToken(),
        tokenService.generateRefreshToken());
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
