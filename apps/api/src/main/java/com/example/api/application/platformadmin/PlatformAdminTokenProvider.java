package com.example.api.application.platformadmin;

import com.example.api.domain.platformadmin.PlatformAdmin;

/**
 * Port for platform admin JWT issuance.
 */
public interface PlatformAdminTokenProvider {

    String generateAccessToken(PlatformAdmin admin);

    String generateRefreshToken(PlatformAdmin admin);

    boolean validateAccessToken(String token);

    boolean validateRefreshToken(String token);

    String getAdminIdFromToken(String token);
}
