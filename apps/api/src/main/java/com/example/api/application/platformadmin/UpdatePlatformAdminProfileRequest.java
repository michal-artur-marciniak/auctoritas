package com.example.api.application.platformadmin;

/**
 * Request to update platform admin profile.
 */
public record UpdatePlatformAdminProfileRequest(
        String email,
        String name,
        String currentPassword,
        String newPassword
) {
}
