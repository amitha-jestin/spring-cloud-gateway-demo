package com.amithapoulose.booking.model;

/**
 * ApiError — consistent error response structure for all booking service errors.
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path
) {}
