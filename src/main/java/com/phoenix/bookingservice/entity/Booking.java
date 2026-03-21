package com.phoenix.bookingservice.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    private String bookingId;
    private String eventId;
    private String userId;
    private String customerEmail;
    private String seat;
    private String ticketType;
    private Integer quantity;
    private BigDecimal totalAmount;

    private String inventoryReservationId;

    private String paymentReferenceId;
    private String paymentTransactionId;

    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;

    private Instant createdAt;
    private Instant updatedAt;
}