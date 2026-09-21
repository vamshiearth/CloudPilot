package com.cloudpilot.backend.exception;

public class FeatureNotAvailableException extends RuntimeException {

    public FeatureNotAvailableException(String message) {
        super(message);
    }
}
