package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrgRegistrationRequest;
import dev.auctoritas.auth.api.OrgRegistrationResponse;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.model.organization.OrgMemberRefreshToken;
import dev.auctoritas.auth.domain.model.organization.Organization;
import dev.auctoritas.auth.domain.model.organization.OrganizationMember;
import dev.auctoritas.auth.domain.model.organization.service.OrganizationRegistrationDomainService;
import dev.auctoritas.auth.domain.model.organization.service.OrganizationRegistrationDomainService.OwnerSpec;
import dev.auctoritas.auth.domain.model.organization.service.OrganizationRegistrationDomainService.RegistrationResult;
import dev.auctoritas.auth.domain.organization.OrgMemberRole;
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
  private final OrganizationRegistrationDomainService domainService;

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
    this.domainService = new OrganizationRegistrationDomainService();
  }

  @Transactional
  public OrgRegistrationResponse register(OrgRegistrationRequest request) {
    // Check slug uniqueness before delegating to domain service
    if (organizationRepository.existsBySlug(request.slug().trim().toLowerCase())) {
      throw new DomainConflictException("org_slug_taken");
    }

    // Delegate to domain service for business logic
    RegistrationResult result = domainService.register(
        request.orgName(),
        request.slug(),
        request.ownerEmail(),
        request.ownerPassword(),
        request.ownerName());

    Organization organization = result.organization();
    OwnerSpec ownerSpec = result.ownerSpec();

    // Application layer handles infrastructure: password hashing and member creation
    String passwordHash = passwordEncoder.encode(ownerSpec.plainPassword());
    OrganizationMember owner = OrganizationMember.create(
        organization,
        ownerSpec.email(),
        passwordHash,
        ownerSpec.name(),
        OrgMemberRole.OWNER,
        true);

    // Add owner to organization
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

  private void persistRefreshToken(OrganizationMember member, String rawToken) {
    OrgMemberRefreshToken token =
        OrgMemberRefreshToken.create(
            member,
            tokenService.hashToken(rawToken),
            java.time.Duration.ofHours(720), // 30 days default
            null, // no IP for registration
            null  // no user agent for registration
        );
    refreshTokenRepository.save(token);

    // Publish and clear domain events
    token.getDomainEvents().forEach(event -> domainEventPublisherPort.publish(event.eventType(), event));
    token.clearDomainEvents();
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
}
