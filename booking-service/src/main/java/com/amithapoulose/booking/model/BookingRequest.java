package com.amithapoulose.booking.model;

/**
 * BookingRequest — inbound payload for POST /booking/bookings.
 */
public record BookingRequest(
        String eventId,
        String userName
) {}
