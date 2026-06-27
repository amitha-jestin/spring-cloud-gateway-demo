package com.amithapoulose.booking.controller;

import com.amithapoulose.booking.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.file.attribute.UserPrincipal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo Booking Service

 * This service is intentionally simplified for demonstration purposes.

 * Security Model:
 * - JWT authentication and validation is handled exclusively by the API Gateway
 * - This service does NOT validate JWT tokens
 * - Authorization is NOT implemented here
 * Purpose:
 * - Demonstrate request flow through API Gateway
 * - Provide a lightweight downstream microservice example

 * ⚠️ This is NOT a production-ready security model and should not be used as-is in real systems.
 */

@Slf4j
@RestController
@RequestMapping("/booking/bookings")
public class BookingController {


    @PostMapping
    public ResponseEntity<?> initiateBooking(
            @RequestBody BookingRequest request,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId
            ) {

        String userId = "123"; // In a real scenario, this would be extracted from the request header or security context

        log.info("Booking confirmed: bookingId={} userId={}", "243", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Booking(
                "243", request.eventId(), userId, "Demo User"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingStatus(
            @PathVariable String id,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        String userId = "123"; // In a real scenario, this would be extracted from the request header or security context

        log.info("GET /booking/bookings/{} userId={} traceId={}", id, userId, traceId);

        return ResponseEntity.ok(new Booking(id, "event-123", userId, "Demo User"));
    }


}
