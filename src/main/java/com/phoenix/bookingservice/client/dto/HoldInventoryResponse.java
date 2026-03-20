package com.phoenix.bookingservice.client.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HoldInventoryResponse {
    private String reservationId;
    private String status;
    private Instant expiresAt;
}