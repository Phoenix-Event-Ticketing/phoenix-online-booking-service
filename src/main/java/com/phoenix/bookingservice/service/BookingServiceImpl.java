package com.phoenix.bookingservice.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.entity.Booking;
import com.phoenix.bookingservice.entity.BookingStatus;
import com.phoenix.bookingservice.entity.PaymentStatus;
import com.phoenix.bookingservice.exception.BookingNotFoundException;
import com.phoenix.bookingservice.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        Instant now = Instant.now();

        Booking booking = Booking.builder()
                .bookingId(generateUniqueBookingId())
                .eventId(request.getEventId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .ticketType(request.getTicketType())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    @Override
    public BookingResponse getBookingByBookingId(String bookingId) {
        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found for ID: " + bookingId));

        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByCustomerEmail(String email) {
        return bookingRepository.findByCustomerEmailIgnoreCase(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private String generateUniqueBookingId() {
        String bookingId;

        do {
            bookingId = "BKG-" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        } while (bookingRepository.existsByBookingId(bookingId));

        return bookingId;
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingId(booking.getBookingId())
                .eventId(booking.getEventId())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .ticketType(booking.getTicketType())
                .quantity(booking.getQuantity())
                .totalAmount(booking.getTotalAmount())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}