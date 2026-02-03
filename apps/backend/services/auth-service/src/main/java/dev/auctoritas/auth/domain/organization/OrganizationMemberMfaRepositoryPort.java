package dev.auctoritas.auth.domain.organization;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for OrganizationMemberMfa persistence operations.
 */
public interface OrganizationMemberMfaRepositoryPort {

  /**
   * Find MFA settings by member ID.
   *
   * @param memberId the member ID
   * @return optional containing the MFA settings if found
   */
  Optional<OrganizationMemberMfa> findByMemberId(UUID memberId);

  /**
   * Find MFA settings by member ID with pessimistic lock for update.
   *
   * @param memberId the member ID
   * @return optional containing the MFA settings if found
   */
  Optional<OrganizationMemberMfa> findByMemberIdForUpdate(UUID memberId);

  /**
   * Save MFA settings.
   *
   * @param mfa the MFA settings to save
   * @return the saved MFA settings
   */
  OrganizationMemberMfa save(OrganizationMemberMfa mfa);

  /**
   * Delete MFA settings.
   *
   * @param mfa the MFA settings to delete
   */
  void delete(OrganizationMemberMfa mfa);

  /**
   * Check if MFA is enabled for a member.
   *
   * @param memberId the member ID
   * @return true if MFA is enabled
   */
  boolean isEnabledByMemberId(UUID memberId);
}
