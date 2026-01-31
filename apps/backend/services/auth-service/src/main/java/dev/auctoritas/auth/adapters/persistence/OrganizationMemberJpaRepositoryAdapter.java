package dev.auctoritas.auth.adapters.persistence;

import dev.auctoritas.auth.domain.organization.OrganizationMemberStatus;
import dev.auctoritas.auth.domain.model.organization.OrganizationMember;
import dev.auctoritas.auth.ports.organization.OrganizationMemberRepositoryPort;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adapter exposing {@link OrganizationMemberRepository} via {@link OrganizationMemberRepositoryPort}.
 */
@Component
public class OrganizationMemberJpaRepositoryAdapter implements OrganizationMemberRepositoryPort {

  private final OrganizationMemberRepository organizationMemberRepository;

  public OrganizationMemberJpaRepositoryAdapter(OrganizationMemberRepository organizationMemberRepository) {
    this.organizationMemberRepository = organizationMemberRepository;
  }

  @Override
  public Optional<OrganizationMember> findByEmailAndOrganizationId(String email, UUID organizationId) {
    return organizationMemberRepository.findByEmailAndOrganizationId(email, organizationId);
  }

  @Override
  public boolean existsByEmailAndOrganizationId(String email, UUID organizationId) {
    return organizationMemberRepository.existsByEmailAndOrganizationId(email, organizationId);
  }

  @Override
  public List<OrganizationMember> findByOrganizationId(UUID organizationId) {
    return organizationMemberRepository.findByOrganizationId(organizationId);
  }

  @Override
  public List<OrganizationMember> findByStatus(OrganizationMemberStatus status) {
    return organizationMemberRepository.findByStatus(status);
  }

  @Override
  public Optional<OrganizationMember> findByIdWithOrganization(UUID id) {
    return organizationMemberRepository.findByIdWithOrganization(id);
  }

  @Override
  public Optional<OrganizationMember> findById(UUID id) {
    return organizationMemberRepository.findById(id);
  }

  @Override
  public OrganizationMember save(OrganizationMember member) {
    return organizationMemberRepository.save(member);
  }
}
