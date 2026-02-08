package com.example.api.domain.platformadmin;

import com.example.api.domain.user.Email;

import java.util.Optional;

/**
 * Repository port for platform admin persistence.
 */
public interface PlatformAdminRepository {

    Optional<PlatformAdmin> findById(PlatformAdminId id);

    Optional<PlatformAdmin> findByEmail(Email email);

    long countByStatus(PlatformAdminStatus status);

    PlatformAdmin save(PlatformAdmin admin);

    void delete(PlatformAdminId id);
}
