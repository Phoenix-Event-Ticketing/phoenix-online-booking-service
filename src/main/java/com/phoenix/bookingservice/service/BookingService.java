package com.phoenix.bookingservice.service;

import java.util.List;

import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.dto.UpdateBookingRequest;
import com.phoenix.bookingservice.dto.PaymentCallbackRequest;
import com.phoenix.bookingservice.dto.StartPaymentResponse;

public interface BookingService {

    BookingResponse createBooking(CreateBookingRequest request);

    List<BookingResponse> getAllBookings();

    BookingResponse updateBooking(String bookingId, UpdateBookingRequest request);

    BookingResponse getBookingByBookingId(String bookingId);

    List<BookingResponse> getBookingsByCustomerEmail(String email);

    StartPaymentResponse startPayment(String bookingId);

    BookingResponse handlePaymentCallback(PaymentCallbackRequest request);

    BookingResponse cancelBooking(String bookingId);

    BookingResponse expireBooking(String bookingId);
}