package dev.auctoritas.auth.dto;

import dev.auctoritas.common.dto.AuthTokens;

public record OrganizationRegistrationResponse(
    OrganizationInfo organization,
    MemberInfo member,
    AuthTokens tokens
) {}
