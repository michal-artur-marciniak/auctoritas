package com.example.api.application.platformadmin;

import com.example.api.application.platformadmin.dto.PlatformAdminAuthResponse;
import com.example.api.application.platformadmin.dto.PlatformAdminResponse;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.platformadmin.PlatformAdminStatus;
import com.example.api.domain.user.Email;
import com.example.api.domain.user.PasswordEncoder;
import com.example.api.domain.user.exception.InvalidCredentialsException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case for platform admin login.
 */
@Component
public class PlatformAdminLoginUseCase {

    private final PlatformAdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAdminTokenProvider tokenProvider;

    public PlatformAdminLoginUseCase(PlatformAdminRepository adminRepository,
                                     PasswordEncoder passwordEncoder,
                                     PlatformAdminTokenProvider tokenProvider) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public PlatformAdminAuthResponse execute(String email, String password) {
        final var admin = adminRepository
                .findByEmail(new Email(email))
                .orElseThrow(InvalidCredentialsException::new);

        if (!admin.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!admin.getPassword().matches(password, passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        admin.recordLogin();
        adminRepository.save(admin);

        final var accessToken = tokenProvider.generateAccessToken(admin);
        final var refreshToken = tokenProvider.generateRefreshToken(admin);

        return new PlatformAdminAuthResponse(
                accessToken,
                refreshToken,
                PlatformAdminResponse.from(admin)
        );
    }
}
