package com.phoenix.bookingservice.dto;

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
public class StartPaymentResponse {
    private String bookingId;
    private String paymentReferenceId;
    private String paymentUrl;
    private BookingStatus bookingStatus;
    private PaymentStatus paymentStatus;
}