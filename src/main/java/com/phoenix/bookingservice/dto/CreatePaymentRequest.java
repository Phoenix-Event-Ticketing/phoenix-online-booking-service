package com.phoenix.bookingservice.client.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreatePaymentRequest {
    private String bookingId;
    private BigDecimal amount;
    private String customerEmail;
    private String callbackUrl;
    private String description;
}