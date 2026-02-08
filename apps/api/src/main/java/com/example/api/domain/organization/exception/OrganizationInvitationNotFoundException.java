package com.example.api.domain.organization.exception;

/**
 * Thrown when an invitation token is invalid.
 */
public class OrganizationInvitationNotFoundException extends RuntimeException {

    public OrganizationInvitationNotFoundException() {
        super("Invitation not found");
    }
}
