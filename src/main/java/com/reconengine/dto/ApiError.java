package com.reconengine.dto;

import java.time.Instant;

public record ApiError(boolean success, int status, String error, String message, String path, Instant timestamp) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(false, status, error, message, path, Instant.now());
    }
}
