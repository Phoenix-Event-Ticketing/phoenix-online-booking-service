package com.phoenix.bookingservice.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.phoenix.bookingservice.entity.BookingStatus;
import com.phoenix.bookingservice.entity.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private String id;
    private String bookingId;
    private String eventId;
    private String userId;
    private String customerEmail;
    private String seat;
    private String ticketType;
    private Integer quantity;
    private BigDecimal totalAmount;

    private String paymentReferenceId;
    private String paymentTransactionId;

    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
    private Instant createdAt;
    private Instant updatedAt;
}