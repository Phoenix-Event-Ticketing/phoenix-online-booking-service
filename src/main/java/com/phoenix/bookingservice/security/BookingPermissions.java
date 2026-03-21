package com.phoenix.bookingservice.security;

public final class BookingPermissions {

    public static final String CREATE_BOOKING = "CREATE_BOOKING";
    public static final String VIEW_BOOKINGS = "VIEW_BOOKINGS";
    public static final String VIEW_ALL_BOOKINGS = "VIEW_ALL_BOOKINGS";
    public static final String UPDATE_BOOKING = "UPDATE_BOOKING";
    public static final String CANCEL_BOOKING = "CANCEL_BOOKING";
    public static final String INTERNAL_SERVICE = "INTERNAL_SERVICE";

    private BookingPermissions() {
    }
}