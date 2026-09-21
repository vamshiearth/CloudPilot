package com.cloudpilot.backend.exception;

public class ProjectHasTasksException extends RuntimeException {

    public ProjectHasTasksException(String message) {
        super(message);
    }
}