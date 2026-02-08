package com.example.api.domain.organization;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository port for organization invitation persistence.
 */
public interface OrganizationInvitationRepository {

    Optional<OrganizationInvitation> findByToken(String token);

    OrganizationInvitation save(OrganizationInvitation invitation);

    void deleteById(OrganizationInvitationId id);

    void deleteExpired(LocalDateTime now);
}
