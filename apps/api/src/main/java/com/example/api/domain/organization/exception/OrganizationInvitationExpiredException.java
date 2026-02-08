package com.example.api.domain.organization.exception;

/**
 * Thrown when an invitation has expired.
 */
public class OrganizationInvitationExpiredException extends RuntimeException {

    public OrganizationInvitationExpiredException() {
        super("Invitation has expired");
    }
}
