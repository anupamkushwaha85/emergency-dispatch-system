package com.emergency.emergency108.auth.exception;

/**
 * Represents the DriverNotVerifiedException component in the system.
 *
 * @author anupam kushwaha
 */
public class DriverNotVerifiedException extends AuthException {

    public DriverNotVerifiedException() {
        super("Driver is not verified");
    }
}
