package com.amithapoulose.booking.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Booking — represents a confirmed, pending, or cancelled ticket booking.
 */
public record Booking(
        String id,
        String eventId,
        String userId,
        String userName
) {}
