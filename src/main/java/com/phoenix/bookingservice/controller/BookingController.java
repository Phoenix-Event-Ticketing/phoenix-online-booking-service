package com.phoenix.bookingservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.dto.PaymentCallbackRequest;
import com.phoenix.bookingservice.dto.StartPaymentRequest;
import com.phoenix.bookingservice.dto.UpdateBookingRequest;
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

    @GetMapping
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/customer/{email}")
    public List<BookingResponse> getBookingsByCustomerEmail(@PathVariable String email) {
        return bookingService.getBookingsByCustomerEmail(email);
    }

    @PatchMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(@PathVariable String bookingId) {
        return bookingService.cancelBooking(bookingId);
    }

    @PatchMapping("/{bookingId}")
    public BookingResponse updateBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        return bookingService.updateBooking(bookingId, request);
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBookingById(@PathVariable String bookingId) {
        return bookingService.getBookingByBookingId(bookingId);
    }

    @PostMapping("/{bookingId}/start-payment")
    public StartPaymentResponse startPayment(
            @PathVariable String bookingId,
            @RequestBody(required = false) StartPaymentRequest request
    ) {
        String paymentMethod = request != null ? request.getPaymentMethod() : null;
        return bookingService.startPayment(bookingId, paymentMethod);
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