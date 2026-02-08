package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationInvitation;
import com.example.api.domain.organization.OrganizationInvitationId;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.user.Email;

/**
 * Maps between domain {@link OrganizationInvitation} and JPA {@link OrganizationInvitationJpaEntity}.
 */
final class OrganizationInvitationDomainMapper {

    private OrganizationInvitationDomainMapper() {
        // Utility class
    }

    static OrganizationInvitation toDomain(OrganizationInvitationJpaEntity entity) {
        final var invitedBy = entity.getInvitedBy() == null || entity.getInvitedBy().isBlank()
                ? null
                : OrganizationMemberId.of(entity.getInvitedBy());
        return new OrganizationInvitation(
                OrganizationInvitationId.of(entity.getId()),
                OrganizationId.of(entity.getOrganizationId()),
                new Email(entity.getEmail()),
                entity.getRole(),
                entity.getToken(),
                invitedBy,
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }

    static OrganizationInvitationJpaEntity toEntity(OrganizationInvitation invitation) {
        return new OrganizationInvitationJpaEntity(
                invitation.getId().value(),
                invitation.getOrganizationId().value(),
                invitation.getEmail().value(),
                invitation.getRole(),
                invitation.getToken(),
                invitation.getInvitedBy() == null ? null : invitation.getInvitedBy().value(),
                invitation.getExpiresAt(),
                invitation.getCreatedAt()
        );
    }
}
