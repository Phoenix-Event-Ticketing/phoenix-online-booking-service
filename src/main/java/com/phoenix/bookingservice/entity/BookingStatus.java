package com.phoenix.bookingservice.entity;

public enum BookingStatus {
    PENDING,
    AWAITING_PAYMENT,
    CONFIRMED,
    FAILED,
    CANCELLED,
    EXPIRED
}