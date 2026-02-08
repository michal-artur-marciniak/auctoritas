package com.example.api.application.platformadmin;

import com.example.api.domain.platformadmin.PlatformAdmin;
import com.example.api.domain.platformadmin.PlatformAdminAlreadyExistsException;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.Password;
import com.example.api.domain.user.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for creating a new platform admin.
 */
@Component
public class CreatePlatformAdminUseCase {

    private final PlatformAdminRepository platformAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public CreatePlatformAdminUseCase(PlatformAdminRepository platformAdminRepository,
                                       PasswordEncoder passwordEncoder) {
        this.platformAdminRepository = platformAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PlatformAdminResponse execute(CreatePlatformAdminRequest request) {
        final var email = new Email(request.email());

        if (platformAdminRepository.findByEmail(email).isPresent()) {
            throw new PlatformAdminAlreadyExistsException(request.email());
        }

        final var password = Password.create(request.password(), passwordEncoder);
        final var admin = PlatformAdmin.create(email, password, request.name());

        platformAdminRepository.save(admin);

        return PlatformAdminResponse.from(admin);
    }
}
