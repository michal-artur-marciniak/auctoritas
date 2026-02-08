package com.example.api.presentation.auth.sdk;

import com.example.api.application.auth.sdk.dto.SdkRegisterRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for SDK end user registration request.
 */
public record SdkRegisterRequestDto(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String name
) {
    SdkRegisterRequest toRequest() {
        return new SdkRegisterRequest(email, password, name);
    }
}
