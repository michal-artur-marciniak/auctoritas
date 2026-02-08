package com.example.api.domain.organization.exception;

/**
 * Thrown when an action would leave an organization without an owner.
 */
public class OrganizationOwnerRequiredException extends RuntimeException {

    public OrganizationOwnerRequiredException() {
        super("Organization must have at least one owner");
    }
}
