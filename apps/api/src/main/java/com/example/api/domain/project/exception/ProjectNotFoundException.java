package com.example.api.domain.project.exception;

/**
 * Exception thrown when a project is not found.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String projectId) {
        super("Project not found: " + projectId);
    }
}
