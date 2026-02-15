package com.example.api.domain.platformadmin;

/**
 * Exception thrown when attempting to deactivate the last active platform admin.
 */
public class CannotDeactivateLastAdminException extends RuntimeException {

    public CannotDeactivateLastAdminException() {
        super("Cannot deactivate the last active platform admin. At least one active admin is required.");
    }
}
