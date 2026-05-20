package com.luciodowglas.userapi.exception;

public class ExternalIntegrationException extends RuntimeException {

    public ExternalIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExternalIntegrationException(String message) {
        super(message);
    }
}
