package dev.auctoritas.auth.service;

import dev.auctoritas.auth.dto.OrganizationRegistrationRequest;
import dev.auctoritas.auth.dto.RegistrationResult;
import dev.auctoritas.auth.dto.UpdateOrganizationRequest;
import dev.auctoritas.auth.entity.organization.Organization;
import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.auth.entity.organization.OrgMemberSession;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import dev.auctoritas.auth.repository.OrganizationRepository;
import dev.auctoritas.auth.repository.OrgMemberSessionRepository;
import dev.auctoritas.common.enums.OrgMemberRole;
import dev.auctoritas.common.enums.OrgMemberStatus;
import dev.auctoritas.common.enums.OrganizationStatus;
import dev.auctoritas.common.exception.ServiceException;
import dev.auctoritas.common.util.ValidationUtils;
import dev.auctoritas.common.validation.PasswordValidator;
import dev.auctoritas.common.validation.ValidationResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final String ERROR_SLUG_UNAVAILABLE = "ORGANIZATION_SLUG_UNAVAILABLE";
    private static final String ERROR_SLUG_INVALID = "ORGANIZATION_SLUG_INVALID";
    private static final String ERROR_ORG_NOT_FOUND = "ORGANIZATION_NOT_FOUND";
    private static final String ERROR_MEMBER_NOT_FOUND = "MEMBER_NOT_FOUND";
    private static final String ERROR_PASSWORD_INVALID = "PASSWORD_INVALID";
    private static final String ERROR_EMAIL_INVALID = "EMAIL_INVALID";
    private static final int SESSION_EXPIRY_HOURS = 24;

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrgMemberSessionRepository orgMemberSessionRepository;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public RegistrationResult register(OrganizationRegistrationRequest req) {
        log.info("Registering new organization: {} with slug: {}", req.organizationName(), req.slug());

        if (!ValidationUtils.isValidSlug(req.slug())) {
            return RegistrationResult.failure("Invalid slug format. Slug must be 3-50 characters, lowercase letters, numbers, and hyphens only.");
        }

        if (!ValidationUtils.isValidEmail(req.email())) {
            return RegistrationResult.failure("Invalid email format");
        }

        ValidationResult passwordValidation = passwordValidator.validate(req.password());
        if (!passwordValidation.valid()) {
            String errorMessage = String.join(", ", passwordValidation.getErrorMessages());
            return RegistrationResult.failure("Password validation failed: " + errorMessage);
        }

        if (organizationRepository.existsBySlug(req.slug())) {
            return RegistrationResult.failure("Organization slug is already in use");
        }

        Organization organization = new Organization();
        organization.setName(req.organizationName());
        organization.setSlug(req.slug());
        organization.setStatus(OrganizationStatus.ACTIVE);

        OrganizationMember ownerMember = new OrganizationMember();
        ownerMember.setOrganization(organization);
        ownerMember.setEmail(req.email());
        ownerMember.setPasswordHash(passwordEncoder.encode(req.password()));
        ownerMember.setName(req.name());
        ownerMember.setRole(OrgMemberRole.OWNER);
        ownerMember.setStatus(OrgMemberStatus.ACTIVE);
        ownerMember.setEmailVerified(false);

        Instant now = Instant.now();
        ownerMember.setCreatedAt(now);
        ownerMember.setUpdatedAt(now);
        organization.setCreatedAt(now);
        organization.setUpdatedAt(now);

        organization.getMembers().add(ownerMember);

        organizationRepository.save(organization);

        OrgMemberSession session = new OrgMemberSession();
        session.setMember(ownerMember);
        session.setExpiresAt(Instant.now().plus(SESSION_EXPIRY_HOURS, ChronoUnit.HOURS));
        orgMemberSessionRepository.save(session);

        log.info("Successfully registered organization: {} with ID: {}", organization.getName(), organization.getId());

        return RegistrationResult.success(
            ownerMember,
            organization.getId(),
            organization.getName(),
            organization.getSlug(),
            organization.getCreatedAt()
        );
    }

    public Organization findBySlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ServiceException("Slug cannot be null or empty", ERROR_SLUG_INVALID);
        }
        return organizationRepository.findBySlug(slug)
            .orElse(null);
    }

    public Organization findById(UUID id) {
        if (id == null) {
            throw new ServiceException("Organization ID cannot be null", ERROR_ORG_NOT_FOUND);
        }
        return organizationRepository.findById(id)
            .orElse(null);
    }

    @Transactional
    public Organization update(UUID id, UpdateOrganizationRequest req) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new ServiceException("Organization not found", ERROR_ORG_NOT_FOUND, id.toString(), "Organization"));

        if (req.name() != null && !req.name().isBlank()) {
            organization.setName(req.name());
        }

        organization.setUpdatedAt(Instant.now());
        return organizationRepository.save(organization);
    }

    public boolean isSlugAvailable(String slug) {
        if (slug == null || slug.isBlank()) {
            return false;
        }
        if (!ValidationUtils.isValidSlug(slug)) {
            return false;
        }
        return !organizationRepository.existsBySlug(slug);
    }

    @Transactional
    public void delete(UUID id) {
        Organization organization = organizationRepository.findById(id)
            .orElseThrow(() -> new ServiceException("Organization not found", ERROR_ORG_NOT_FOUND, id.toString(), "Organization"));

        organization.setStatus(OrganizationStatus.DELETE);

        List<OrganizationMember> members = organizationMemberRepository.findByOrganizationId(id);
        for (OrganizationMember member : members) {
            member.setStatus(OrgMemberStatus.DELETED);
            member.setUpdatedAt(Instant.now());
            organizationMemberRepository.save(member);
        }

        organization.setUpdatedAt(Instant.now());
        organizationRepository.save(organization);

        log.info("Deleted organization: {} with ID: {}", organization.getName(), id);
    }
}
