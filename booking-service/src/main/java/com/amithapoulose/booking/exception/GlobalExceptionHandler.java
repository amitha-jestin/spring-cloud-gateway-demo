package com.amithapoulose.booking.exception;

import com.amithapoulose.booking.model.ApiError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;

/**
 * GlobalExceptionHandler — Converts uncaught exceptions to consistent ApiError responses.
 *
 * Every error returned by this service follows the same JSON shape, regardless
 * of where in the call stack it originated. This makes client-side error handling
 * predictable and keeps error responses traceable via timestamp and path.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException ex, WebRequest req) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now().toString(),
                400,
                "Bad Request",
                ex.getMessage(),
                req.getDescription(false)
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex, WebRequest req) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                Instant.now().toString(),
                500,
                "Internal Server Error",
                "An unexpected error occurred. Please contact support if this persists.",
                req.getDescription(false)
        ));
    }
}
