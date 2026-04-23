package com.phoenix.bookingservice.repository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.phoenix.bookingservice.entity.Booking;
import com.phoenix.bookingservice.entity.BookingStatus;
import com.phoenix.bookingservice.entity.PaymentStatus;

public interface BookingRepository extends MongoRepository<Booking, String> {

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByCustomerEmailIgnoreCase(String customerEmail);

    boolean existsByBookingId(String bookingId);

    List<Booking> findByBookingStatusInAndPaymentStatusAndUpdatedAtBefore(
            List<BookingStatus> bookingStatuses,
            PaymentStatus paymentStatus,
            Instant updatedAtBefore
    );
}