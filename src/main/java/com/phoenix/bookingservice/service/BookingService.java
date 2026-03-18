package com.phoenix.bookingservice.service;

import java.util.List;

import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    BookingResponse getBookingByBookingId(String bookingId);

    List<BookingResponse> getBookingsByCustomerEmail(String email);
}