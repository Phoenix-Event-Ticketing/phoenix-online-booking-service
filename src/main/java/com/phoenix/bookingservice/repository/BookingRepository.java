package com.phoenix.bookingservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.phoenix.bookingservice.entity.Booking;

public interface BookingRepository extends MongoRepository<Booking, String> {

    Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByCustomerEmailIgnoreCase(String customerEmail);

    boolean existsByBookingId(String bookingId);
}