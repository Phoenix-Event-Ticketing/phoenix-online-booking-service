package com.phoenix.bookingservice.client.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HoldInventoryResponse {
    private String bookingId;
    private String holdStatus;
    private Instant expiresAt;
}