package com.example.api.presentation.auth.sdk;

import com.example.api.application.auth.sdk.dto.SdkLoginRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for SDK end user login request.
 */
public record SdkLoginRequestDto(
        @NotBlank @Email String email,
        @NotBlank String password
) {
    SdkLoginRequest toRequest() {
        return new SdkLoginRequest(email, password);
    }
}
