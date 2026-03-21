package com.phoenix.bookingservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventSummaryResponse {
    private String eventId;
    private String name;
    private Boolean active;
}