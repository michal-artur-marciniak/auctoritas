package com.example.api.application.auth.sdk.dto;

import com.example.api.application.auth.dto.UserDto;

/**
 * Output DTO for SDK authentication responses.
 */
public record SdkAuthResponse(String accessToken, String refreshToken, UserDto user) {
}
