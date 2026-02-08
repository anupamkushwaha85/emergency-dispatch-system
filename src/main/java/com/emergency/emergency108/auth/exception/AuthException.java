package com.emergency.emergency108.auth.exception;

public abstract class AuthException extends RuntimeException {

    protected AuthException(String message) {
        super(message);
    }
}
