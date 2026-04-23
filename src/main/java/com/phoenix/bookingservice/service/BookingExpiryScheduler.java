package com.phoenix.bookingservice.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingExpiryScheduler {

    private final BookingServiceImpl bookingService;

    @Scheduled(fixedDelayString = "${booking.expiry-scan-interval-ms:60000}")
    public void expireStalePendingBookings() {
        bookingService.expireStalePendingBookings();
    }
}
