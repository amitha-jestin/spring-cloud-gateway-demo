package com.amithapoulose.booking.controller;

import com.amithapoulose.booking.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BookingController — REST resource for ticket booking operations.
 *
 * Authorization is enforced via gateway-propagated headers (not JWT).
 * The gateway validated the JWT; this service trusts its headers.
 *
 * POST /booking/bookings   — requires X-User-Id header (any authenticated user)
 * GET  /booking/bookings/{id} — requires X-User-Id header
 * GET  /booking/bookings/user/{userId} — user can only see own bookings
 * DELETE /booking/bookings/{id}/cancel — requires X-User-Id header
 */
@Slf4j
@RestController
@RequestMapping("/booking/bookings")
public class BookingController {


    @PostMapping
    public ResponseEntity<?> initiateBooking(
            @RequestBody BookingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        log.info("POST /booking/bookings eventId={} userId={} traceId={}",
                request.eventId(), userId, traceId);

        // Authorization — X-User-Id must be present (injected by gateway after JWT validation)
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(
                    Instant.now().toString(), 401, "Unauthorized",
                    "User identity required. Ensure request passes through the API Gateway.",
                    "/booking/bookings"));
        }


        log.info("Booking confirmed: bookingId={} userId={}", "243", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(new Booking(
                "243", request.eventId(), userId, "Demo User"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingStatus(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Trace-Id", required = false) String traceId) {

        log.info("GET /booking/bookings/{} userId={} traceId={}", id, userId, traceId);

        if (userId == null || userId.isBlank()) {
            return ResponseEntity.status(401).body(new ApiError(
                    Instant.now().toString(), 401, "Unauthorized",
                    "User identity required.", "/booking/bookings/" + id));
        }


        return ResponseEntity.ok(new Booking(id, "event-123", userId, "Demo User"));
    }


}
