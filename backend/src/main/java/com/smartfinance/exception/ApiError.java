package com.smartfinance.exception;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;

public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String message, String path,
                              Map<String, String> fieldErrors) {
        return new ApiError(Instant.now().toString(), status.value(),
                status.getReasonPhrase(), message, path, fieldErrors);
    }

}
