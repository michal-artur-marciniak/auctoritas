package dev.auctoritas.auth.application.port.in.org;

import dev.auctoritas.auth.adapter.in.web.OrgRegistrationRequest;
import dev.auctoritas.auth.adapter.in.web.OrgRegistrationResponse;

/**
 * Use case for Organization registration.
 */
public interface OrganizationRegistrationUseCase {
  OrgRegistrationResponse register(OrgRegistrationRequest request);
}
