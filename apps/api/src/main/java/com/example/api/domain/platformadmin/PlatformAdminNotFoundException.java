package com.example.api.domain.platformadmin;

/**
 * Exception thrown when a platform admin is not found.
 */
public class PlatformAdminNotFoundException extends RuntimeException {

    public PlatformAdminNotFoundException(String message) {
        super(message);
    }

    public static PlatformAdminNotFoundException byId(PlatformAdminId id) {
        return new PlatformAdminNotFoundException("Platform admin not found: " + id.value());
    }

    public static PlatformAdminNotFoundException byEmail(String email) {
        return new PlatformAdminNotFoundException("Platform admin not found with email: " + email);
    }
}
