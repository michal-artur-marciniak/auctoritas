package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;

/**
 * Maps between domain {@link OrganizationMember} and JPA {@link OrganizationMemberJpaEntity}.
 */
final class OrganizationMemberDomainMapper {

    private OrganizationMemberDomainMapper() {
        // Utility class
    }

    static OrganizationMember toDomain(OrganizationMemberJpaEntity entity) {
        return new OrganizationMember(
                OrganizationMemberId.of(entity.getId()),
                OrganizationId.of(entity.getOrganizationId()),
                new Email(entity.getEmail()),
                Password.fromHash(entity.getPasswordHash()),
                entity.getName(),
                entity.getRole(),
                entity.isEmailVerified(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    static OrganizationMemberJpaEntity toEntity(OrganizationMember member) {
        return new OrganizationMemberJpaEntity(
                member.getId().value(),
                member.getOrganizationId().value(),
                member.getEmail().value(),
                member.getPassword().hashedValue(),
                member.getName(),
                member.getRole(),
                member.isEmailVerified(),
                member.getStatus(),
                member.getLastLoginAt(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
