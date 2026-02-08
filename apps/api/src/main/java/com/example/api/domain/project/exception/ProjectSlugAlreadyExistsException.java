package com.example.api.domain.project.exception;

/**
 * Exception thrown when a project slug already exists within an organization.
 */
public class ProjectSlugAlreadyExistsException extends RuntimeException {

    public ProjectSlugAlreadyExistsException(String slug) {
        super("Project slug already exists: " + slug);
    }
}
