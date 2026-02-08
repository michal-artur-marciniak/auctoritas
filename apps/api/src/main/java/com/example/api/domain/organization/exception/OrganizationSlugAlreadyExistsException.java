package com.example.api.domain.organization.exception;

/**
 * Thrown when an organization slug is already in use.
 */
public class OrganizationSlugAlreadyExistsException extends RuntimeException {

    public OrganizationSlugAlreadyExistsException(String slug) {
        super("Organization slug already exists: " + slug);
    }
}
