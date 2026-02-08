package com.emergency.emergency108.auth.exception;

public class UserBlockedException extends AuthException {

    public UserBlockedException() {
        super("User account is blocked");
    }
}
