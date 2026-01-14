package dev.auctoritas.auth.dto;

import dev.auctoritas.auth.entity.organization.OrganizationMember;
import dev.auctoritas.common.dto.AuthTokens;

public record LoginResponse(
    OrganizationMember member,
    AuthTokens tokens,
    boolean mfaRequired,
    String mfaToken
) {}
