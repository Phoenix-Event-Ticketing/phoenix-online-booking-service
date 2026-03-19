package com.phoenix.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentResponse {
    private String paymentReferenceId;
    private String paymentUrl;
    private String status;
}