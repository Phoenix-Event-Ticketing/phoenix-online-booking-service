package com.phoenix.bookingservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.client.dto.HoldInventoryRequest;
import com.phoenix.bookingservice.client.dto.HoldInventoryResponse;
import com.phoenix.bookingservice.client.dto.InventoryAvailabilityResponse;
import com.phoenix.bookingservice.client.dto.ReleaseInventoryRequest;
import com.phoenix.bookingservice.exception.BusinessValidationException;
import com.phoenix.bookingservice.exception.ExternalServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.inventory.base-url}")
    private String inventoryServiceBaseUrl;

    public void checkAvailability(String eventId, String ticketType, Integer quantity) {
        String url = inventoryServiceBaseUrl
                + "/inventory/availability?eventId={eventId}&ticketType={ticketType}";

        try {
            ResponseEntity<InventoryAvailabilityResponse> response = restTemplate.getForEntity(
                    url,
                    InventoryAvailabilityResponse.class,
                    eventId,
                    ticketType
            );

            InventoryAvailabilityResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new ExternalServiceException("Invalid response received from Inventory Service");
            }

            boolean available = Boolean.TRUE.equals(body.getAvailable());
            int availableQuantity = body.getAvailableQuantity() == null ? 0 : body.getAvailableQuantity();

            if (!available || availableQuantity < quantity) {
                throw new BusinessValidationException("Requested ticket quantity is not available");
            }

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessValidationException("Ticket inventory not found for the selected event and ticket type");
            }

            throw new ExternalServiceException("Inventory Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to communicate with Inventory Service", ex);
        }
    }

    public HoldInventoryResponse holdTickets(String bookingId, String eventId, String ticketType, Integer quantity) {
        String url = inventoryServiceBaseUrl + "/inventory/hold";

        HoldInventoryRequest request = new HoldInventoryRequest(
                bookingId,
                eventId,
                ticketType,
                quantity
        );

        try {
            ResponseEntity<HoldInventoryResponse> response =
                    restTemplate.postForEntity(url, request, HoldInventoryResponse.class);

            HoldInventoryResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.getReservationId() == null) {
                throw new ExternalServiceException("Invalid hold response received from Inventory Service");
            }

            return body;

        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException("Inventory hold request failed: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to reserve tickets through Inventory Service", ex);
        }
    }

    public void releaseTickets(String reservationId, String bookingId) {
        String url = inventoryServiceBaseUrl + "/inventory/release";

        ReleaseInventoryRequest request = new ReleaseInventoryRequest(reservationId, bookingId);

        try {
            restTemplate.postForEntity(url, request, Void.class);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to release reserved tickets through Inventory Service", ex);
        }
    }
}