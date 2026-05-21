package com.luciodowglas.userapi.exception;

public class InsufficientPrivilegesException extends RuntimeException {

    public InsufficientPrivilegesException(String message) {
        super(message);
    }
}
