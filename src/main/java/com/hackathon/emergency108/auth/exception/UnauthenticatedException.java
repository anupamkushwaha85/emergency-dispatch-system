package com.hackathon.emergency108.auth.exception;

public class UnauthenticatedException extends AuthException {

    public UnauthenticatedException() {
        super("Authentication required");
    }
}