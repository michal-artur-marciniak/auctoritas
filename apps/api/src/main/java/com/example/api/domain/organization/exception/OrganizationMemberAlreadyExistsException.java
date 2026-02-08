package com.example.api.domain.organization.exception;

/**
 * Thrown when a member email is already registered in the organization.
 */
public class OrganizationMemberAlreadyExistsException extends RuntimeException {

    public OrganizationMemberAlreadyExistsException(String email) {
        super("Organization member already exists: %s".formatted(email));
    }
}
