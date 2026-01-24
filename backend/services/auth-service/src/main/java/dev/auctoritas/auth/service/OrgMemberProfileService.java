package dev.auctoritas.auth.service;

import dev.auctoritas.auth.api.OrgMemberProfileResponse;
import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.auth.repository.OrganizationMemberRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for org member profile operations.
 */
@Service
public class OrgMemberProfileService {
  private final OrganizationMemberRepository memberRepository;

  public OrgMemberProfileService(OrganizationMemberRepository memberRepository) {
    this.memberRepository = memberRepository;
  }

  /**
   * Gets the profile of the currently authenticated org member.
   *
   * @param memberId the ID of the authenticated org member
   * @return the member's profile with organization info
   * @throws IllegalArgumentException if member not found
   */
  @Transactional(readOnly = true)
  public OrgMemberProfileResponse getProfile(UUID memberId) {
    OrganizationMember member =
        memberRepository
            .findByIdWithOrganization(memberId)
            .orElseThrow(() -> new IllegalArgumentException("Member not found"));

    return new OrgMemberProfileResponse(
        member.getId(),
        member.getEmail(),
        member.getName(),
        member.getAvatarUrl(),
        member.getRole(),
        member.getStatus(),
        member.getEmailVerified(),
        member.getMfa() != null && member.getMfa().getEnabled(),
        member.getCreatedAt(),
        new OrgMemberProfileResponse.OrganizationInfo(
            member.getOrganization().getId(),
            member.getOrganization().getName(),
            member.getOrganization().getSlug()));
  }
}
