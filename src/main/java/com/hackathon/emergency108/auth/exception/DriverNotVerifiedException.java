package com.hackathon.emergency108.auth.exception;

public class DriverNotVerifiedException extends AuthException {

    public DriverNotVerifiedException() {
        super("Driver is not verified");
    }
}
