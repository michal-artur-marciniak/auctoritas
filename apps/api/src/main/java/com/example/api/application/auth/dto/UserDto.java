package com.example.api.application.auth.dto;

import com.example.api.domain.user.User;

/**
 * Output DTO for user data in auth responses.
 */
public record UserDto(String id, String email, String name, String role) {

    /**
     * Creates a UserDto from a domain User aggregate.
     */
    public static UserDto fromDomain(User user) {
        return new UserDto(
                user.getId().value(),
                user.getEmail().value(),
                user.getName(),
                user.getRole().name()
        );
    }
}
