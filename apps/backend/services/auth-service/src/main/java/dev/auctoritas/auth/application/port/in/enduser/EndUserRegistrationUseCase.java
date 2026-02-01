package dev.auctoritas.auth.application.port.in.enduser;

import dev.auctoritas.auth.application.enduser.EndUserRegistrationCommand;
import dev.auctoritas.auth.application.enduser.EndUserRegistrationResult;

/**
 * Use case for EndUser registration.
 */
public interface EndUserRegistrationUseCase {
  EndUserRegistrationResult register(
      String apiKey,
      EndUserRegistrationCommand command,
      String ipAddress,
      String userAgent);
}
