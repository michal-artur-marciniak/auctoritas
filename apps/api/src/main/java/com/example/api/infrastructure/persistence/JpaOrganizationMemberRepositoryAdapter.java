package com.example.api.infrastructure.persistence;

import com.example.api.domain.organization.OrganizationId;
import com.example.api.domain.organization.OrganizationMember;
import com.example.api.domain.organization.OrganizationMemberId;
import com.example.api.domain.organization.OrganizationMemberRepository;
import com.example.api.domain.organization.OrganizationMemberRole;
import com.example.api.domain.user.Email;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter implementing the domain {@link OrganizationMemberRepository} port.
 */
@Repository
public class JpaOrganizationMemberRepositoryAdapter implements OrganizationMemberRepository {

    private final OrganizationMemberJpaRepository jpaRepository;

    public JpaOrganizationMemberRepositoryAdapter(OrganizationMemberJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<OrganizationMember> findById(OrganizationMemberId id) {
        return jpaRepository.findById(id.value())
                .map(OrganizationMemberDomainMapper::toDomain);
    }

    @Override
    public Optional<OrganizationMember> findByEmailAndOrganizationId(Email email, OrganizationId organizationId) {
        return jpaRepository.findByEmailAndOrganizationId(email.value(), organizationId.value())
                .map(OrganizationMemberDomainMapper::toDomain);
    }

    @Override
    public long countByOrganizationIdAndRole(OrganizationId organizationId, OrganizationMemberRole role) {
        return jpaRepository.countByOrganizationIdAndRole(organizationId.value(), role);
    }

    @Override
    public List<OrganizationMember> findByOrganizationId(OrganizationId organizationId) {
        return jpaRepository.findAllByOrganizationId(organizationId.value()).stream()
                .map(OrganizationMemberDomainMapper::toDomain)
                .toList();
    }

    @Override
    public OrganizationMember save(OrganizationMember member) {
        final var entity = OrganizationMemberDomainMapper.toEntity(member);
        jpaRepository.save(entity);
        return member;
    }

    @Override
    public void delete(OrganizationMemberId id) {
        jpaRepository.deleteById(id.value());
    }
}
