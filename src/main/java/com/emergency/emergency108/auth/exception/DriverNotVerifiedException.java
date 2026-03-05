package com.emergency.emergency108.auth.exception;

/**
 * @author anupam kushwaha
 */
public class DriverNotVerifiedException extends AuthException {

    public DriverNotVerifiedException() {
        super("Driver is not verified");
    }
}
