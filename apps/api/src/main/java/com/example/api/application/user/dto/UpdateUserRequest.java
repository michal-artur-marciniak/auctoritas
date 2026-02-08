package com.example.api.application.user.dto;

/**
 * Application-layer request for updating user profile data.
 */
public record UpdateUserRequest(String userId, String email, String name) {
}
