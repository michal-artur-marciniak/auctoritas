package dev.auctoritas.auth.dto;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record UpdateOrganizationRequest(
    @Size(min = 3, max = 100, message = "Organization name must be between 3 and 100 characters")
    String name
) {}
