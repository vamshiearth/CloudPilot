package com.cloudpilot.backend.audit;

public class AuditServiceUnavailableException extends RuntimeException {

    public AuditServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
