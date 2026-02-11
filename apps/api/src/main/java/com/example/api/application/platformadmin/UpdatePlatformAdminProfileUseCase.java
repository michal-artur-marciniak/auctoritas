package com.example.api.application.platformadmin;

import com.example.api.domain.platformadmin.PlatformAdminAlreadyExistsException;
import com.example.api.domain.platformadmin.PlatformAdminId;
import com.example.api.domain.platformadmin.PlatformAdminNotFoundException;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Use case for updating the current platform admin profile.
 */
@Component
public class UpdatePlatformAdminProfileUseCase {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public UpdatePlatformAdminProfileUseCase(PlatformAdminRepository platformAdminRepository,
                                              PasswordEncoder passwordEncoder) {
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PlatformAdminResponse execute(String adminId, UpdatePlatformAdminProfileRequest request) {
        final var admin = platformAdminRepository
                .findById(PlatformAdminId.of(adminId))
                .orElseThrow(() -> new PlatformAdminNotFoundException(adminId));

        // Update name if provided
        if (request.name() != null && !request.name().isBlank()) {
            admin.updateProfile(request.name());
        }

        // Update email if provided (with uniqueness check)
        if (request.email() != null && !request.email().isBlank()) {
            final var newEmail = new Email(request.email());
            if (!newEmail.equals(admin.getEmail())) {
                platformAdminRepository.findByEmail(newEmail).ifPresent(existing -> {
                    throw new PlatformAdminAlreadyExistsException(request.email());
                });
                admin.changeEmail(newEmail);
            }
        }

        // Update password if provided (requires current password verification)
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new IllegalArgumentException("Current password required to change password");
            }
            if (!admin.getPassword().matches(request.currentPassword(), passwordEncoder)) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            final var newPassword = Password.create(request.newPassword(), passwordEncoder);
            admin.changePassword(newPassword);
        }

        platformAdminRepository.save(admin);

        return PlatformAdminResponse.from(admin);
    }
}
