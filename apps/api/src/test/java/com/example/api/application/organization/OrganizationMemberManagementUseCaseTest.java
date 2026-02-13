package com.example.api.application.organization;

import com.example.api.application.organization.dto.AcceptInvitationRequest;
import com.example.api.application.organization.dto.InviteMemberRequest;
import com.example.api.application.organization.dto.UpdateMemberRoleRequest;
import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationInvitation;
import com.example.api.domain.organization.OrganizationInvitationId;
import com.example.api.domain.organization.OrganizationInvitationRepository;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationMemberRole;
import com.example.api.domain.organization.OrganizationRepository;
import com.example.api.domain.organization.OrganizationStatus;
import com.example.api.domain.organization.exception.OrganizationInvitationExpiredException;
import com.example.api.domain.organization.exception.OrganizationInvitationNotFoundException;
import com.example.api.domain.organization.exception.OrganizationMemberAlreadyExistsException;
import com.example.api.domain.organization.exception.OrganizationMemberNotFoundException;
import com.example.api.domain.organization.exception.OrganizationOwnerRequiredException;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberManagementUseCaseTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationMemberRepository memberRepository;

    @Mock
    private OrganizationInvitationRepository invitationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OrganizationMemberManagementUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new OrganizationMemberManagementUseCase(
                organizationRepository,
                memberRepository,
                invitationRepository,
                passwordEncoder,
                72
        );
    }

    @Test
    void inviteMemberRequiresOwnerOrAdmin() {
        final var actor = sampleMember(OrganizationMemberRole.MEMBER);
        when(memberRepository.findById(any())).thenReturn(Optional.of(actor));
        // No need to stub organizationRepository since role check fails first

        assertThrows(IllegalArgumentException.class, () ->
                useCase.inviteMember(new InviteMemberRequest(
                        actor.getOrganizationId().value(),
                        actor.getId().value(),
                        "new@acme.com",
                        OrganizationMemberRole.MEMBER)));
    }

    @Test
    void inviteMemberRejectsDuplicateEmail() {
        final var actor = sampleMember(OrganizationMemberRole.ADMIN);
        when(memberRepository.findById(any())).thenReturn(Optional.of(actor));
        when(organizationRepository.findById(actor.getOrganizationId())).thenReturn(Optional.of(sampleOrganization()));
        when(memberRepository.findByEmailAndOrganizationId(any(), any())).thenReturn(Optional.of(actor));

        assertThrows(OrganizationMemberAlreadyExistsException.class, () ->
                useCase.inviteMember(new InviteMemberRequest(
                        actor.getOrganizationId().value(),
                        actor.getId().value(),
                        "owner@acme.com",
                        OrganizationMemberRole.MEMBER)));
    }

    @Test
    void acceptInvitationCreatesMemberAndDeletesInvitation() {
        final var invitation = sampleInvitation();
        when(invitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));
        when(memberRepository.findByEmailAndOrganizationId(any(), any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed");

        final var response = useCase.acceptInvitation(new AcceptInvitationRequest(
                invitation.getOrganizationId().value(),
                "token",
                "New Member",
                "password123"));

        assertEquals("new@acme.com", response.email());
        verify(memberRepository).save(any());
        verify(invitationRepository).deleteById(invitation.getId());
    }

    @Test
    void acceptInvitationRejectsExpiredToken() {
        final var invitation = expiredInvitation();
        when(invitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));

        assertThrows(OrganizationInvitationExpiredException.class, () ->
                useCase.acceptInvitation(new AcceptInvitationRequest(
                        invitation.getOrganizationId().value(),
                        "token",
                        "New Member",
                        "password123")));
    }

    @Test
    void acceptInvitationRejectsWrongOrganization() {
        final var invitation = sampleInvitation();
        when(invitationRepository.findByToken("token")).thenReturn(Optional.of(invitation));

        assertThrows(OrganizationInvitationNotFoundException.class, () ->
                useCase.acceptInvitation(new AcceptInvitationRequest(
                        "other-org",
                        "token",
                        "New Member",
                        "password123")));
    }

    @Test
    void changeRoleRequiresOwner() {
        final var actor = sampleMember(OrganizationMemberRole.ADMIN);
        when(memberRepository.findById(any())).thenReturn(Optional.of(actor));
        // No need to stub organizationRepository since role check fails first

        assertThrows(IllegalArgumentException.class, () ->
                useCase.changeRole(new UpdateMemberRoleRequest(
                        actor.getOrganizationId().value(),
                        actor.getId().value(),
                        "member-id",
                        OrganizationMemberRole.MEMBER)));
    }

    @Test
    void changeRolePreventsLastOwnerRemoval() {
        final var actor = sampleMember(OrganizationMemberRole.OWNER);
        final var target = sampleMember(OrganizationMemberRole.OWNER);
        when(memberRepository.findById(eq(actor.getId()))).thenReturn(Optional.of(actor));
        when(memberRepository.findById(eq(target.getId()))).thenReturn(Optional.of(target));
        // No need to stub organizationRepository since count check happens before organization check
        when(memberRepository.countByOrganizationIdAndRole(actor.getOrganizationId(), OrganizationMemberRole.OWNER))
                .thenReturn(1L);

        assertThrows(OrganizationOwnerRequiredException.class, () ->
                useCase.changeRole(new UpdateMemberRoleRequest(
                        actor.getOrganizationId().value(),
                        actor.getId().value(),
                        target.getId().value(),
                        OrganizationMemberRole.ADMIN)));
    }

    @Test
    void removeMemberPreventsLastOwnerRemoval() {
        final var actor = sampleMember(OrganizationMemberRole.OWNER);
        final var target = sampleMember(OrganizationMemberRole.OWNER);
        when(memberRepository.findById(eq(actor.getId()))).thenReturn(Optional.of(actor));
        when(memberRepository.findById(eq(target.getId()))).thenReturn(Optional.of(target));
        // No need to stub organizationRepository since count check happens before organization check
        when(memberRepository.countByOrganizationIdAndRole(actor.getOrganizationId(), OrganizationMemberRole.OWNER))
                .thenReturn(1L);

        assertThrows(OrganizationOwnerRequiredException.class, () ->
                useCase.removeMember(actor.getOrganizationId().value(), actor.getId().value(), target.getId().value()));
    }

    @Test
    void removeMemberRequiresSameOrganization() {
        final var actor = sampleMember(OrganizationMemberRole.OWNER);
        final var target = sampleMember(OrganizationMemberRole.MEMBER, OrganizationId.of("other-org"));
        when(memberRepository.findById(eq(actor.getId()))).thenReturn(Optional.of(actor));
        when(memberRepository.findById(eq(target.getId()))).thenReturn(Optional.of(target));
        // No need to stub organizationRepository since org mismatch check happens first

        assertThrows(OrganizationMemberNotFoundException.class, () ->
                useCase.removeMember(actor.getOrganizationId().value(), actor.getId().value(), target.getId().value()));
        verify(memberRepository, never()).delete(any());
    }

    private OrganizationMember sampleMember(OrganizationMemberRole role) {
        return sampleMember(role, OrganizationId.of("org-id"));
    }

    private OrganizationMember sampleMember(OrganizationMemberRole role, OrganizationId organizationId) {
        return new OrganizationMember(
                OrganizationMemberId.generate(),
                organizationId,
                new Email("owner@acme.com"),
                Password.fromHash("hashed"),
                "Owner",
                role,
                false,
                OrganizationStatus.ACTIVE,
                null,
                LocalDateTime.now(),
                null
        );
    }

    private OrganizationInvitation sampleInvitation() {
        return new OrganizationInvitation(
                OrganizationInvitationId.generate(),
                OrganizationId.of("org-id"),
                new Email("new@acme.com"),
                OrganizationMemberRole.MEMBER,
                "token",
                OrganizationMemberId.of("actor-id"),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now()
        );
    }

    private OrganizationInvitation expiredInvitation() {
        return new OrganizationInvitation(
                OrganizationInvitationId.generate(),
                OrganizationId.of("org-id"),
                new Email("new@acme.com"),
                OrganizationMemberRole.MEMBER,
                "token",
                OrganizationMemberId.of("actor-id"),
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().minusDays(1)
        );
    }

    private com.example.api.domain.organization.Organization sampleOrganization() {
        return new com.example.api.domain.organization.Organization(
                OrganizationId.of("org-id"),
                "Acme",
                "acme",
                OrganizationStatus.ACTIVE,
                LocalDateTime.now(),
                null
        );
    }
}
