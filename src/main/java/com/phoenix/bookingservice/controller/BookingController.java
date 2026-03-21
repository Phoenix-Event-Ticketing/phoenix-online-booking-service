package com.phoenix.bookingservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.dto.PaymentCallbackRequest;
import com.phoenix.bookingservice.dto.StartPaymentResponse;
import com.phoenix.bookingservice.service.BookingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@PathVariable String bookingId) {
        return bookingService.getBookingByBookingId(bookingId);
    }

    @GetMapping("/customer/{email}")
    public List<BookingResponse> getBookingsByCustomerEmail(@PathVariable String email) {
        return bookingService.getBookingsByCustomerEmail(email);
    }

    @PatchMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(@PathVariable String bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    @PostMapping("/{bookingId}/start-payment")
    public StartPaymentResponse startPayment(@PathVariable String bookingId) {
        return bookingService.startPayment(bookingId);
    }

    @PostMapping("/{bookingId}/expire")
    public BookingResponse expireBooking(@PathVariable String bookingId) {
        return bookingService.expireBooking(bookingId);
    }

    @PostMapping("/payment-callback")
    public BookingResponse handlePaymentCallback(@Valid @RequestBody PaymentCallbackRequest request) {
        return bookingService.handlePaymentCallback(request);
    }
}