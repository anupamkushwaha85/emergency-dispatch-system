package com.emergency.emergency108.auth.exception;

/**
 * Represents the UserBlockedException component in the system.
 *
 * @author anupam kushwaha
 */
public class UserBlockedException extends AuthException {

    public UserBlockedException() {
        super("User account is blocked");
    }
}
