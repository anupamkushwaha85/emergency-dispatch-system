package com.emergency.emergency108.auth.exception;

/**
 * @author anupam kushwaha
 */
public class UnauthenticatedException extends AuthException {

    public UnauthenticatedException() {
        super("Authentication required");
    }
}