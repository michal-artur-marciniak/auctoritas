package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrgRegistrationRequest;
import dev.auctoritas.auth.api.OrgRegistrationResponse;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import dev.auctoritas.auth.domain.model.organization.OrgMemberRefreshToken;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrgMemberRole;
import dev.auctoritas.auth.domain.valueobject.Email;
import dev.auctoritas.auth.domain.valueobject.Slug;
import dev.auctoritas.auth.ports.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.ports.organization.OrgMemberRefreshTokenRepositoryPort;
import dev.auctoritas.auth.ports.organization.OrganizationMemberRepositoryPort;
import dev.auctoritas.auth.ports.organization.OrganizationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationRegistrationService {
  private static final Logger log = LoggerFactory.getLogger(OrganizationRegistrationService.class);

  private final OrganizationRepositoryPort organizationRepository;
  private final OrganizationMemberRepositoryPort organizationMemberRepository;
  private final OrgMemberRefreshTokenRepositoryPort refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;

  public OrganizationRegistrationService(
      OrganizationRepositoryPort organizationRepository,
      OrganizationMemberRepositoryPort organizationMemberRepository,
      OrgMemberRefreshTokenRepositoryPort refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort) {
    this.organizationRepository = organizationRepository;
    this.organizationMemberRepository = organizationMemberRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
  }

  @Transactional
  public OrgRegistrationResponse register(OrgRegistrationRequest request) {
    // Validate and create slug value object
    Slug slug = createSlug(request.slug());

    if (organizationRepository.existsBySlug(slug.value())) {
      throw new DomainConflictException("org_slug_taken");
    }

    // Create organization using factory method
    Organization organization = Organization.create(request.orgName(), slug);

    // Create and add owner member
    OrganizationMember owner = createOwnerMember(organization, request);
    organization.addMember(owner);

    // Save organization (cascades to members due to CascadeType.ALL)
    Organization savedOrganization = organizationRepository.save(organization);
    OrganizationMember savedMember = savedOrganization.getMembers().get(0);

    // Publish domain events
    publishDomainEvents(savedOrganization, savedMember);

    // Generate tokens
    String rawRefreshToken = tokenService.generateRefreshToken();
    persistRefreshToken(savedMember, rawRefreshToken);

    String accessToken =
        jwtService.generateAccessToken(
            savedMember.getId(),
            savedOrganization.getId(),
            savedMember.getEmail(),
            savedMember.getRole());

    log.info(
        "organization_registered organizationId={} memberId={}",
        savedOrganization.getId(),
        savedMember.getId());

    return new OrgRegistrationResponse(
        new OrgRegistrationResponse.OrganizationSummary(
            savedOrganization.getId(), savedOrganization.getName(), savedOrganization.getSlug()),
        new OrgRegistrationResponse.MemberSummary(
            savedMember.getId(), savedMember.getEmail(), savedMember.getRole()),
        accessToken,
        rawRefreshToken);
  }

  private Slug createSlug(String slugValue) {
    if (slugValue == null || slugValue.trim().isEmpty()) {
      throw new DomainValidationException("org_slug_required");
    }
    return Slug.of(slugValue.trim().toLowerCase());
  }

  private OrganizationMember createOwnerMember(Organization organization, OrgRegistrationRequest request) {
    if (request.ownerEmail() == null || request.ownerEmail().trim().isEmpty()) {
      throw new DomainValidationException("owner_email_required");
    }
    if (request.ownerPassword() == null || request.ownerPassword().trim().isEmpty()) {
      throw new DomainValidationException("owner_password_required");
    }

    Email email = Email.of(request.ownerEmail());
    String passwordHash = passwordEncoder.encode(request.ownerPassword().trim());
    String name = trimToNull(request.ownerName());

    return OrganizationMember.create(
        organization,
        email,
        passwordHash,
        name,
        OrgMemberRole.OWNER,
        true);
  }

  private void persistRefreshToken(OrganizationMember member, String rawToken) {
    OrgMemberRefreshToken token = new OrgMemberRefreshToken();
    token.setMember(member);
    token.setTokenHash(tokenService.hashToken(rawToken));
    token.setExpiresAt(tokenService.getRefreshTokenExpiry());
    token.setRevoked(false);
    refreshTokenRepository.save(token);
  }

  private void publishDomainEvents(Organization organization, OrganizationMember member) {
    // Publish organization events
    organization.getDomainEvents().forEach(event -> {
      try {
        domainEventPublisherPort.publish(event.eventType(), event);
      } catch (RuntimeException ex) {
        log.warn("Failed to publish organization event: {}", event.eventType(), ex);
      }
    });
    organization.clearDomainEvents();

    // Publish member events
    member.getDomainEvents().forEach(event -> {
      try {
        domainEventPublisherPort.publish(event.eventType(), event);
      } catch (RuntimeException ex) {
        log.warn("Failed to publish member event: {}", event.eventType(), ex);
      }
    });
    member.clearDomainEvents();
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
