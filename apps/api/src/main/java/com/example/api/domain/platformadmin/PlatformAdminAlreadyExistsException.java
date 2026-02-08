package com.example.api.domain.platformadmin;

/**
 * Exception thrown when attempting to create a platform admin with an email that already exists.
 */
public class PlatformAdminAlreadyExistsException extends RuntimeException {

    public PlatformAdminAlreadyExistsException(String email) {
        super("Platform admin already exists with email: " + email);
    }
}
