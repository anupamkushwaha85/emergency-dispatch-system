package com.emergency.emergency108.auth.exception;

/**
 * Represents the UnauthenticatedException component in the system.
 *
 * @author anupam kushwaha
 */
public class UnauthenticatedException extends AuthException {

    public UnauthenticatedException() {
        super("Authentication required");
    }
}