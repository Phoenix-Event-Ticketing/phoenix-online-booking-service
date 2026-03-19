package com.phoenix.bookingservice.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.phoenix.bookingservice.client.EventServiceClient;
import com.phoenix.bookingservice.client.InventoryServiceClient;
import com.phoenix.bookingservice.client.PaymentServiceClient;
import com.phoenix.bookingservice.client.dto.CreatePaymentResponse;
import com.phoenix.bookingservice.client.dto.HoldInventoryResponse;
import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.dto.PaymentCallbackRequest;
import com.phoenix.bookingservice.dto.StartPaymentResponse;
import com.phoenix.bookingservice.entity.Booking;
import com.phoenix.bookingservice.entity.BookingStatus;
import com.phoenix.bookingservice.entity.PaymentStatus;
import com.phoenix.bookingservice.exception.BookingNotFoundException;
import com.phoenix.bookingservice.exception.BusinessValidationException;
import com.phoenix.bookingservice.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventServiceClient eventServiceClient;
    private final InventoryServiceClient inventoryServiceClient;
    private final PaymentServiceClient paymentServiceClient;

    @Override
    public BookingResponse createBooking(CreateBookingRequest request) {
        eventServiceClient.verifyEventExistsAndIsActive(request.getEventId());
        inventoryServiceClient.checkAvailability(
                request.getEventId(),
                request.getTicketType(),
                request.getQuantity()
        );

        String bookingId = generateUniqueBookingId();

        HoldInventoryResponse holdResponse = inventoryServiceClient.holdTickets(
                bookingId,
                request.getEventId(),
                request.getTicketType(),
                request.getQuantity()
        );

        Instant now = Instant.now();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .eventId(request.getEventId())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .ticketType(request.getTicketType())
                .quantity(request.getQuantity())
                .totalAmount(request.getTotalAmount())
                .inventoryReservationId(holdResponse.getReservationId())
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            Booking savedBooking = bookingRepository.save(booking);
            return mapToResponse(savedBooking);
        } catch (RuntimeException ex) {
            safelyReleaseReservation(holdResponse.getReservationId(), bookingId);
            throw ex;
        }
    }

    @Override
    public BookingResponse getBookingByBookingId(String bookingId) {
        Booking booking = findBookingEntityByBookingId(bookingId);
        return mapToResponse(booking);
    }

    @Override
    public List<BookingResponse> getBookingsByCustomerEmail(String email) {
        return bookingRepository.findByCustomerEmailIgnoreCase(email)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public StartPaymentResponse startPayment(String bookingId) {
        Booking booking = findBookingEntityByBookingId(bookingId);

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            throw new BusinessValidationException("Payment has already been completed for this booking");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.EXPIRED
                || booking.getBookingStatus() == BookingStatus.FAILED) {
            throw new BusinessValidationException("Payment cannot be started for the current booking state");
        }

        if (booking.getInventoryReservationId() == null || booking.getInventoryReservationId().isBlank()) {
            throw new BusinessValidationException("Booking does not have a valid inventory reservation");
        }

        if (booking.getPaymentReferenceId() != null
                && !booking.getPaymentReferenceId().isBlank()
                && booking.getBookingStatus() == BookingStatus.AWAITING_PAYMENT
                && booking.getPaymentStatus() == PaymentStatus.PENDING) {
            return mapToStartPaymentResponse(booking);
        }

        CreatePaymentResponse paymentResponse = paymentServiceClient.createPayment(booking);

        booking.setPaymentReferenceId(paymentResponse.getPaymentReferenceId());
        booking.setPaymentUrl(paymentResponse.getPaymentUrl());
        booking.setBookingStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setUpdatedAt(Instant.now());

        Booking savedBooking = bookingRepository.save(booking);
        return mapToStartPaymentResponse(savedBooking);
    }

    @Override
    public BookingResponse handlePaymentCallback(PaymentCallbackRequest request) {
        Booking booking = findBookingEntityByBookingId(request.getBookingId());

        if (booking.getPaymentReferenceId() == null || booking.getPaymentReferenceId().isBlank()) {
            throw new BusinessValidationException("Booking does not have an active payment reference");
        }

        if (!booking.getPaymentReferenceId().equals(request.getPaymentReferenceId())) {
            throw new BusinessValidationException("Payment reference ID does not match the booking");
        }

        String normalizedStatus = request.getPaymentStatus().trim().toUpperCase();

        switch (normalizedStatus) {
            case "SUCCESS" -> handleSuccessfulPayment(booking, request);
            case "FAILED" -> handleFailedPayment(booking, request);
            case "PENDING" -> handlePendingPayment(booking, request);
            default -> throw new BusinessValidationException(
                    "Unsupported payment callback status: " + request.getPaymentStatus());
        }

        booking.setUpdatedAt(Instant.now());
        Booking savedBooking = bookingRepository.save(booking);
        return mapToResponse(savedBooking);
    }

    private void handleSuccessfulPayment(Booking booking, PaymentCallbackRequest request) {
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED
                && booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return;
        }

        inventoryServiceClient.confirmTickets(
                booking.getInventoryReservationId(),
                booking.getBookingId()
        );

        booking.setPaymentStatus(PaymentStatus.SUCCESS);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentTransactionId(request.getTransactionId());
    }

    private void handleFailedPayment(Booking booking, PaymentCallbackRequest request) {
        safelyReleaseReservation(
                booking.getInventoryReservationId(),
                booking.getBookingId()
        );

        booking.setPaymentStatus(PaymentStatus.FAILED);
        booking.setBookingStatus(BookingStatus.FAILED);
        booking.setPaymentTransactionId(request.getTransactionId());
    }

    private void handlePendingPayment(Booking booking, PaymentCallbackRequest request) {
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingStatus(BookingStatus.AWAITING_PAYMENT);
        booking.setPaymentTransactionId(request.getTransactionId());
    }

    private Booking findBookingEntityByBookingId(String bookingId) {
        return bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found for ID: " + bookingId));
    }

    private void safelyReleaseReservation(String reservationId, String bookingId) {
        try {
            if (reservationId != null && !reservationId.isBlank()) {
                inventoryServiceClient.releaseTickets(reservationId, bookingId);
            }
        } catch (Exception ignored) {
            // preserve original failure
        }
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
                .paymentReferenceId(booking.getPaymentReferenceId())
                .paymentUrl(booking.getPaymentUrl())
                .paymentTransactionId(booking.getPaymentTransactionId())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private StartPaymentResponse mapToStartPaymentResponse(Booking booking) {
        return StartPaymentResponse.builder()
                .bookingId(booking.getBookingId())
                .paymentReferenceId(booking.getPaymentReferenceId())
                .paymentUrl(booking.getPaymentUrl())
                .bookingStatus(booking.getBookingStatus())
                .paymentStatus(booking.getPaymentStatus())
                .build();
    }
}