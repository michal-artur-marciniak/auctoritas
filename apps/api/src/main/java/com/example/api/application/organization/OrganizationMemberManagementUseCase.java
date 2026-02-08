package com.example.api.application.organization;

import com.example.api.application.organization.dto.AcceptInvitationRequest;
import com.example.api.application.organization.dto.InvitationResponse;
import com.example.api.application.organization.dto.InviteMemberRequest;
import com.example.api.application.organization.dto.OrganizationMemberResponse;
import com.example.api.application.organization.dto.UpdateMemberRoleRequest;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationInvitation;
import com.example.api.domain.organization.OrganizationInvitationRepository;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationMemberRole;
import com.example.api.domain.organization.OrganizationMemberRoleAccess;
import com.example.api.domain.organization.OrganizationRepository;
import com.example.api.domain.organization.exception.OrganizationInvitationExpiredException;
import com.example.api.domain.organization.exception.OrganizationInvitationNotFoundException;
import com.example.api.domain.organization.exception.OrganizationMemberAlreadyExistsException;
import com.example.api.domain.organization.exception.OrganizationMemberNotFoundException;
import com.example.api.domain.organization.exception.OrganizationNotFoundException;
import com.example.api.domain.organization.exception.OrganizationOwnerRequiredException;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Use case for organization member invite, accept, role changes, and removals.
 */
@Component
public class OrganizationMemberManagementUseCase {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final OrganizationInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final int invitationExpiryHours;

    public OrganizationMemberManagementUseCase(OrganizationRepository organizationRepository,
                                               OrganizationMemberRepository memberRepository,
                                               OrganizationInvitationRepository invitationRepository,
                                               PasswordEncoder passwordEncoder,
                                               @Value("${app.org.invitation-expiry-hours:72}") int invitationExpiryHours) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.invitationExpiryHours = invitationExpiryHours;
    }

    @Transactional
    public InvitationResponse inviteMember(InviteMemberRequest request) {
        final var organizationId = OrganizationId.of(request.organizationId());
        final var actor = loadMember(OrganizationMemberId.of(request.invitedByMemberId()));
        OrganizationMemberRoleAccess.requireOwnerOrAdmin(actor);
        if (!actor.getOrganizationId().equals(organizationId)) {
            throw new OrganizationMemberNotFoundException(request.invitedByMemberId());
        }

        verifyOrganizationExists(organizationId);

        final var email = new Email(request.email());
        if (memberRepository.findByEmailAndOrganizationId(email, organizationId).isPresent()) {
            throw new OrganizationMemberAlreadyExistsException(request.email());
        }

        final var expiresAt = LocalDateTime.now().plusHours(invitationExpiryHours);
        final var invitation = OrganizationInvitation.create(
                organizationId,
                email,
                request.role(),
                actor.getId(),
                expiresAt,
                MemberTokenGenerator.generate()
        );

        invitationRepository.save(invitation);
        return InvitationResponse.from(invitation);
    }

    @Transactional
    public OrganizationMemberResponse acceptInvitation(AcceptInvitationRequest request) {
        final var invitation = invitationRepository.findByToken(request.token())
                .orElseThrow(OrganizationInvitationNotFoundException::new);

        if (!invitation.getOrganizationId().value().equals(request.organizationId())) {
            throw new OrganizationInvitationNotFoundException();
        }

        if (invitation.isExpired(LocalDateTime.now())) {
            throw new OrganizationInvitationExpiredException();
        }

        if (memberRepository.findByEmailAndOrganizationId(invitation.getEmail(), invitation.getOrganizationId())
                .isPresent()) {
            throw new OrganizationMemberAlreadyExistsException(invitation.getEmail().value());
        }

        final var member = OrganizationMember.createMember(
                invitation.getOrganizationId(),
                invitation.getEmail(),
                Password.create(request.password(), passwordEncoder),
                request.name(),
                invitation.getRole()
        );

        memberRepository.save(member);
        invitationRepository.deleteById(invitation.getId());
        return OrganizationMemberResponse.from(member);
    }

    @Transactional
    public OrganizationMemberResponse changeRole(UpdateMemberRoleRequest request) {
        final var actor = loadMember(OrganizationMemberId.of(request.actorMemberId()));
        OrganizationMemberRoleAccess.requireOwner(actor);
        if (!actor.getOrganizationId().value().equals(request.organizationId())) {
            throw new OrganizationMemberNotFoundException(request.actorMemberId());
        }

        final var target = loadMember(OrganizationMemberId.of(request.memberId()));
        if (!target.getOrganizationId().equals(actor.getOrganizationId())) {
            throw new OrganizationMemberNotFoundException(request.memberId());
        }

        final var currentRole = target.getRole();
        if (currentRole == OrganizationMemberRole.OWNER && request.role() != OrganizationMemberRole.OWNER) {
            final var ownerCount = memberRepository.countByOrganizationIdAndRole(
                    target.getOrganizationId(), OrganizationMemberRole.OWNER);
            if (ownerCount <= 1) {
                throw new OrganizationOwnerRequiredException();
            }
        }

        target.changeRole(request.role());
        memberRepository.save(target);
        return OrganizationMemberResponse.from(target);
    }

    @Transactional
    public void removeMember(String organizationId, String actorMemberId, String memberId) {
        final var actor = loadMember(OrganizationMemberId.of(actorMemberId));
        OrganizationMemberRoleAccess.requireOwnerOrAdmin(actor);
        if (!actor.getOrganizationId().value().equals(organizationId)) {
            throw new OrganizationMemberNotFoundException(actorMemberId);
        }

        final var target = loadMember(OrganizationMemberId.of(memberId));
        if (!target.getOrganizationId().equals(actor.getOrganizationId())) {
            throw new OrganizationMemberNotFoundException(memberId);
        }

        if (target.getRole() == OrganizationMemberRole.OWNER) {
            final var ownerCount = memberRepository.countByOrganizationIdAndRole(
                    target.getOrganizationId(), OrganizationMemberRole.OWNER);
            if (ownerCount <= 1) {
                throw new OrganizationOwnerRequiredException();
            }
        }

        memberRepository.delete(target.getId());
    }

    private OrganizationMember loadMember(OrganizationMemberId memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new OrganizationMemberNotFoundException(memberId.value()));
    }

    private void verifyOrganizationExists(OrganizationId organizationId) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId.value()));
    }
}
