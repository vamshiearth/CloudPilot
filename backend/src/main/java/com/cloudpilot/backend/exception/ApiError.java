package com.cloudpilot.backend.exception;

import java.time.LocalDateTime;

public record ApiError(
                String timestamp,
        int status,
        String error,
        String message,
        String path
) {

        public ApiError(
                        LocalDateTime timestamp,
                        int status,
                        String error,
                        String message,
                        String path) {

                this(timestamp.toString(), status, error, message, path);
        }
}