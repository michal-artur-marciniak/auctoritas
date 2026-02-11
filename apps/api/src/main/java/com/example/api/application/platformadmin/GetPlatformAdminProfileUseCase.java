package com.example.api.application.platformadmin;

import com.example.api.domain.platformadmin.PlatformAdminId;
import com.example.api.domain.platformadmin.PlatformAdminNotFoundException;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for getting the current platform admin profile.
 */
@Component
public class GetPlatformAdminProfileUseCase {

    private final PlatformAdminRepository platformAdminRepository;

    public GetPlatformAdminProfileUseCase(PlatformAdminRepository platformAdminRepository) {
        this.platformAdminRepository = platformAdminRepository;
    }

    @Transactional(readOnly = true)
    public PlatformAdminResponse execute(String adminId) {
        final var admin = platformAdminRepository
                .findById(PlatformAdminId.of(adminId))
                .orElseThrow(() -> new PlatformAdminNotFoundException(adminId));

        return PlatformAdminResponse.from(admin);
    }
}
