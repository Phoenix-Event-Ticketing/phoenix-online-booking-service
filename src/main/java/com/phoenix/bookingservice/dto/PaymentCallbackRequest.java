package com.phoenix.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCallbackRequest {

    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    @NotBlank(message = "Payment reference ID is required")
    private String paymentReferenceId;

    @NotBlank(message = "Payment status is required")
    private String paymentStatus;

    private String transactionId;
}