package dev.auctoritas.auth.domain.mfa;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for EndUserMfa persistence operations.
 * Repository port in the domain layer following Hexagonal Architecture.
 */
public interface EndUserMfaRepositoryPort {

  /**
   * Find MFA settings by user ID.
   *
   * @param userId the user ID
   * @return optional containing the MFA settings if found
   */
  Optional<EndUserMfa> findByUserId(UUID userId);

  /**
   * Find MFA settings by user ID with pessimistic lock.
   *
   * @param userId the user ID
   * @return optional containing the MFA settings if found
   */
  Optional<EndUserMfa> findByUserIdForUpdate(UUID userId);

  /**
   * Save MFA settings.
   *
   * @param mfa the MFA settings to save
   * @return the saved MFA settings
   */
  EndUserMfa save(EndUserMfa mfa);

  /**
   * Delete MFA settings.
   *
   * @param mfa the MFA settings to delete
   */
  void delete(EndUserMfa mfa);

  /**
   * Check if MFA is enabled for a user.
   *
   * @param userId the user ID
   * @return true if MFA is enabled
   */
  boolean isEnabledByUserId(UUID userId);
}
