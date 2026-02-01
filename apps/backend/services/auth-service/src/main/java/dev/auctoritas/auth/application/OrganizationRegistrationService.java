package dev.auctoritas.auth.application;

import dev.auctoritas.auth.adapter.in.web.OrgRegistrationRequest;
import dev.auctoritas.auth.adapter.in.web.OrgRegistrationResponse;
import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshToken;
import dev.auctoritas.auth.domain.organization.Organization;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationRegistrationDomainService;
import dev.auctoritas.auth.domain.organization.OrganizationRegistrationDomainService.OwnerSpec;
import dev.auctoritas.auth.domain.organization.OrganizationRegistrationDomainService.RegistrationResult;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRole;
import dev.auctoritas.auth.application.port.out.messaging.DomainEventPublisherPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRefreshTokenRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRepositoryPort;
import dev.auctoritas.auth.domain.organization.OrganizationRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrganizationRegistrationService implements dev.auctoritas.auth.application.port.in.org.OrganizationRegistrationUseCase {
  private static final Logger log = LoggerFactory.getLogger(OrganizationRegistrationService.class);

  private final OrganizationRepositoryPort organizationRepository;
  private final OrganizationMemberRepositoryPort organizationMemberRepository;
  private final OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final TokenService tokenService;
  private final JwtService jwtService;
  private final DomainEventPublisherPort domainEventPublisherPort;
  private final OrganizationRegistrationDomainService domainService;
  private final TransactionTemplate transactionTemplate;

  public OrganizationRegistrationService(
      OrganizationRepositoryPort organizationRepository,
      OrganizationMemberRepositoryPort organizationMemberRepository,
      OrganizationMemberRefreshTokenRepositoryPort refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      TokenService tokenService,
      JwtService jwtService,
      DomainEventPublisherPort domainEventPublisherPort,
      OrganizationRegistrationDomainService domainService,
      PlatformTransactionManager transactionManager) {
    this.organizationRepository = organizationRepository;
    this.organizationMemberRepository = organizationMemberRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.tokenService = tokenService;
    this.jwtService = jwtService;
    this.domainEventPublisherPort = domainEventPublisherPort;
    this.domainService = domainService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public OrgRegistrationResponse register(OrgRegistrationRequest request) {
    RegistrationContext context =
        transactionTemplate.execute(status -> registerInTransaction(request));
    if (context == null) {
      throw new IllegalStateException("organization_registration_failed");
    }

    String accessToken =
        jwtService.generateAccessToken(
            context.memberId(),
            context.organizationId(),
            context.memberEmail(),
            context.memberRole());

    log.info(
        "organization_registered organizationId={} memberId={}",
        context.organizationId(),
        context.memberId());

    return new OrgRegistrationResponse(
        new OrgRegistrationResponse.OrganizationSummary(
            context.organizationId(), context.organizationName(), context.organizationSlug()),
        new OrgRegistrationResponse.MemberSummary(
            context.memberId(), context.memberEmail(), context.memberRole()),
        accessToken,
        context.rawRefreshToken());
  }

  private RegistrationContext registerInTransaction(OrgRegistrationRequest request) {
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
        OrganizationMemberRole.OWNER,
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

    return new RegistrationContext(
        savedOrganization.getId(),
        savedOrganization.getName(),
        savedOrganization.getSlug(),
        savedMember.getId(),
        savedMember.getEmail(),
        savedMember.getRole(),
        rawRefreshToken);
  }

  private record RegistrationContext(
      java.util.UUID organizationId,
      String organizationName,
      String organizationSlug,
      java.util.UUID memberId,
      String memberEmail,
      OrganizationMemberRole memberRole,
      String rawRefreshToken) {}

  private void persistRefreshToken(OrganizationMember member, String rawToken) {
    OrganizationMemberRefreshToken token =
        OrganizationMemberRefreshToken.create(
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
