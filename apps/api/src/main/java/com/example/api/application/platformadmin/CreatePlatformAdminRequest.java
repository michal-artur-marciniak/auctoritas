package com.example.api.application.platformadmin;

/**
 * Request to create a new platform admin.
 */
public record CreatePlatformAdminRequest(
        String email,
        String password,
        String name
) {
}
