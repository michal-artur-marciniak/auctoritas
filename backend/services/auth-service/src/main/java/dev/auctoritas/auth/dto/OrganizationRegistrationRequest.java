package dev.auctoritas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrganizationRegistrationRequest(
    @NotBlank(message = "Organization name is required")
    @Size(min = 3, max = 100, message = "Organization name must be between 3 and 100 characters")
    String organizationName,

    @NotBlank(message = "Slug is required")
    @Size(min = 3, max = 50, message = "Slug must be between 3 and 50 characters")
    String slug,

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    String password,

    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name
) {}
