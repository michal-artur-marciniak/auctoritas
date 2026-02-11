package com.example.api.application.platformadmin;

import com.example.api.application.platformadmin.dto.PlatformAdminAuthResponse;
import com.example.api.application.platformadmin.dto.PlatformAdminResponse;
import com.example.api.domain.platformadmin.PlatformAdminId;
import com.example.api.domain.platformadmin.PlatformAdminRepository;
import com.example.api.domain.user.exception.InvalidTokenException;
import org.springframework.stereotype.Component;

/**
 * Use case for refreshing platform admin access token.
 */
@Component
public class RefreshPlatformAdminTokenUseCase {

    private final PlatformAdminRepository adminRepository;
    private final PlatformAdminTokenProvider tokenProvider;

    public RefreshPlatformAdminTokenUseCase(PlatformAdminRepository adminRepository,
                                            PlatformAdminTokenProvider tokenProvider) {
        this.adminRepository = adminRepository;
        this.tokenProvider = tokenProvider;
    }

    public PlatformAdminAuthResponse execute(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new InvalidTokenException();
        }

        final var adminId = tokenProvider.getAdminIdFromToken(refreshToken);
        final var admin = adminRepository
                .findById(PlatformAdminId.of(adminId))
                .orElseThrow(InvalidTokenException::new);

        if (!admin.isActive()) {
            throw new InvalidTokenException();
        }

        final var newAccessToken = tokenProvider.generateAccessToken(admin);
        final var newRefreshToken = tokenProvider.generateRefreshToken(admin);

        return new PlatformAdminAuthResponse(
                newAccessToken,
                newRefreshToken,
                PlatformAdminResponse.from(admin)
        );
    }
}
