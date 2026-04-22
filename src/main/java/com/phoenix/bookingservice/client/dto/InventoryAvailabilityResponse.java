package com.phoenix.bookingservice.client.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryAvailabilityResponse {
    private String eventId;
    private List<AvailabilityItem> items;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AvailabilityItem {
        private String inventoryId;
        private String ticketType;
        private BigDecimal price;
        private Integer totalQuantity;
        private Integer heldQuantity;
        private Integer soldQuantity;
        private Integer availableQuantity;
    }
}