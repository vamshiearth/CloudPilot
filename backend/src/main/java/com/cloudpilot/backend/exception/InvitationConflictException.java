package com.cloudpilot.backend.exception;

public class InvitationConflictException extends RuntimeException {

    public InvitationConflictException(String message) {
        super(message);
    }
}