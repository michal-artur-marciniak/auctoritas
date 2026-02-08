package com.example.api.domain.organization.exception;

/**
 * Thrown when a member is not found in the organization.
 */
public class OrganizationMemberNotFoundException extends RuntimeException {

    public OrganizationMemberNotFoundException() {
        super("Organization member not found");
    }

    public OrganizationMemberNotFoundException(String memberId) {
        super("Organization member not found: %s".formatted(memberId));
    }
}
