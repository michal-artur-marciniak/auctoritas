package com.example.api.application.platformadmin;

import com.example.api.domain.platformadmin.CannotDeactivateLastAdminException;
import com.example.api.domain.platformadmin.PlatformAdminId;
import com.example.api.domain.platformadmin.PlatformAdminNotFoundException;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.platformadmin.PlatformAdminStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for deactivating a platform admin.
 */
@Component
public class DeactivatePlatformAdminUseCase {

    private final PlatformAdminRepository platformAdminRepository;

    public DeactivatePlatformAdminUseCase(PlatformAdminRepository platformAdminRepository) {
        this.platformAdminRepository = platformAdminRepository;
    }

    @Transactional
    public void execute(String adminId) {
        final var id = PlatformAdminId.of(adminId);
        
        final var admin = platformAdminRepository.findById(id)
                .orElseThrow(() -> new PlatformAdminNotFoundException(adminId));

        // Check if this is the last active admin
        final var activeAdminCount = platformAdminRepository.countByStatus(PlatformAdminStatus.ACTIVE);
        if (activeAdminCount <= 1 && admin.isActive()) {
            throw new CannotDeactivateLastAdminException();
        }

        admin.deactivate();
        platformAdminRepository.save(admin);
    }
}
