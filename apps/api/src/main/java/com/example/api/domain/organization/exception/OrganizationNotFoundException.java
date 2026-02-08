package com.example.api.domain.organization.exception;

/**
 * Thrown when an organization cannot be found.
 */
public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(String organizationId) {
        super("Organization not found: %s".formatted(organizationId));
    }
}
