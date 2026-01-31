package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrganizationMemberProfileResponse;
import dev.auctoritas.auth.domain.organization.OrganizationMember;
import dev.auctoritas.auth.domain.organization.OrganizationMemberRepositoryPort;
import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for org member profile operations.
 */
@Service
public class OrganizationMemberProfileService {
  private final OrganizationMemberRepositoryPort memberRepository;

  public OrganizationMemberProfileService(OrganizationMemberRepositoryPort memberRepository) {
    this.memberRepository = memberRepository;
  }

  /**
   * Gets the profile of the currently authenticated org member.
   *
   * @param memberId the ID of the authenticated org member
   * @return the member's profile with organization info
   * @throws EntityNotFoundException if member not found
   */
  @Transactional(readOnly = true)
  public OrganizationMemberProfileResponse getProfile(UUID memberId) {
    OrganizationMember member =
        memberRepository
            .findByIdWithOrganization(memberId)
            .orElseThrow(() -> new EntityNotFoundException("Member not found: " + memberId));

    return new OrganizationMemberProfileResponse(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getAvatarUrl(),
        member.getRole(),
        member.getStatus(),
        member.getEmailVerified(),
        member.getMfa() != null && member.getMfa().getEnabled(),
        member.getCreatedAt(),
        new OrganizationMemberProfileResponse.OrganizationInfo(
            member.getOrganization().getId(),
            member.getOrganization().getName(),
            member.getOrganization().getSlug()));
  }
}
