package com.example.portalpartners.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp,
        String path
) {
    public static ErrorResponse of(String message, int status, String path) {
        return new ErrorResponse(message, status, LocalDateTime.now(), path);
    }
}
