package dev.auctoritas.auth.application.port.in.org;

import dev.auctoritas.auth.adapter.in.web.OrganizationMemberProfileResponse;
import java.util.UUID;

/**
 * Use case for retrieving Organization member profiles.
 */
public interface OrganizationMemberProfileUseCase {
  OrganizationMemberProfileResponse getProfile(UUID memberId);
}
