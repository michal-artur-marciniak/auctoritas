package com.example.api.domain.user.exception;

/**
 * Thrown when a banned user attempts to login.
 */
public class UserBannedException extends RuntimeException {

    public UserBannedException() {
        super("User account is banned");
    }
}
