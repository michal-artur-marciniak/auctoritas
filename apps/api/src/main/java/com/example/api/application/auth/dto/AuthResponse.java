package com.example.api.application.auth.dto;

/**
 * Output DTO for authentication responses.
 */
public record AuthResponse(String accessToken, String refreshToken, UserDto user) {
}
