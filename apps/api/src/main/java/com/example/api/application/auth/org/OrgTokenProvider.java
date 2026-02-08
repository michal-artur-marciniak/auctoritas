package com.example.api.application.auth.org;

import com.example.api.domain.organization.OrganizationMember;

/**
 * Port for organization member JWT issuance.
 */
public interface OrgTokenProvider {

    String generateAccessToken(OrganizationMember member);
}
