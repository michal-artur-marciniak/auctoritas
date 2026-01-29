package dev.auctoritas.auth.application.enduser;

/**
 * Command payload for registering an end user.
 *
 * @param email end-user email address
 * @param password raw end-user password
 * @param name optional display name
 */
public record EndUserRegistrationCommand(String email, String password, String name) {}
