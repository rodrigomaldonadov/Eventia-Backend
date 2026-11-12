package com.backend.eventia.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String code,
        String path,
        Map<String, String> errors
) {

    // Constructor compatible con los errores existentes de GlobalExceptionHandler.
    public ErrorResponse(int status, String message, String path, Map<String, String> errors) {
        this(status, message, null, path, errors);
    }

    public ErrorResponse(int status, String message, String code, String path, Map<String, String> errors) {
        this(
                OffsetDateTime.now(),
                status,
                HttpStatus.valueOf(status).getReasonPhrase(),
                message,
                code,
                path,
                errors
        );
    }
}
