package com.phoenix.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryAvailabilityResponse {
    private String eventId;
    private String ticketType;
    private Boolean available;
    private Integer availableQuantity;
}