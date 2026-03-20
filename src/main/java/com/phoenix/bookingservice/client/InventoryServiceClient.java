package com.phoenix.bookingservice.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.client.dto.ConfirmInventoryRequest;
import com.phoenix.bookingservice.client.dto.HoldInventoryRequest;
import com.phoenix.bookingservice.client.dto.HoldInventoryResponse;
import com.phoenix.bookingservice.client.dto.InventoryAvailabilityResponse;
import com.phoenix.bookingservice.client.dto.ReleaseInventoryRequest;
import com.phoenix.bookingservice.exception.BusinessValidationException;
import com.phoenix.bookingservice.exception.ExternalServiceException;
import com.phoenix.bookingservice.logging.StructuredLogger;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceClient {

    private static final StructuredLogger log = StructuredLogger.getLogger(InventoryServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.inventory.base-url}")
    private String inventoryServiceBaseUrl;

    public void checkAvailability(String eventId, String ticketType, Integer quantity) {
        String url = inventoryServiceBaseUrl
                + "/inventory/availability?eventId={eventId}&ticketType={ticketType}";

        log.info("calling inventory service for availability check", Map.of(
                "targetService", "inventory-service",
                "eventId", eventId,
                "ticketType", ticketType,
                "quantity", quantity
        ));

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

            log.info("inventory availability check succeeded", Map.of(
                    "targetService", "inventory-service",
                    "eventId", eventId,
                    "ticketType", ticketType,
                    "availableQuantity", availableQuantity
            ));

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessValidationException("Ticket inventory not found for the selected event and ticket type");
            }

            log.error("inventory service returned an error", Map.of(
                    "targetService", "inventory-service",
                    "eventId", eventId,
                    "ticketType", ticketType,
                    "statusCode", ex.getStatusCode().value()
            ), ex);

            throw new ExternalServiceException("Inventory Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("inventory service communication failed", Map.of(
                    "targetService", "inventory-service",
                    "eventId", eventId,
                    "ticketType", ticketType
            ), ex);

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

        log.info("calling inventory service to hold tickets", Map.of(
                "targetService", "inventory-service",
                "bookingId", bookingId,
                "eventId", eventId,
                "ticketType", ticketType,
                "quantity", quantity
        ));

        try {
            ResponseEntity<HoldInventoryResponse> response =
                    restTemplate.postForEntity(url, request, HoldInventoryResponse.class);

            HoldInventoryResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.getReservationId() == null) {
                throw new ExternalServiceException("Invalid hold response received from Inventory Service");
            }

            log.info("inventory hold succeeded", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId,
                    "reservationId", body.getReservationId()
            ));

            return body;

        } catch (HttpStatusCodeException ex) {
            log.error("inventory hold failed with downstream error", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId,
                    "statusCode", ex.getStatusCode().value()
            ), ex);

            throw new ExternalServiceException("Inventory hold request failed: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("inventory hold communication failed", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId
            ), ex);

            throw new ExternalServiceException("Failed to reserve tickets through Inventory Service", ex);
        }
    }

    public void confirmTickets(String reservationId, String bookingId) {
        String url = inventoryServiceBaseUrl + "/inventory/confirm";

        ConfirmInventoryRequest request = new ConfirmInventoryRequest(reservationId, bookingId);

        log.info("calling inventory service to confirm held tickets", Map.of(
                "targetService", "inventory-service",
                "bookingId", bookingId,
                "reservationId", reservationId
        ));

        try {
            restTemplate.postForEntity(url, request, Void.class);

            log.info("inventory confirmation succeeded", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId,
                    "reservationId", reservationId
            ));
        } catch (RestClientException ex) {
            log.error("inventory confirmation failed", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId,
                    "reservationId", reservationId
            ), ex);

            throw new ExternalServiceException("Failed to confirm reserved tickets through Inventory Service", ex);
        }
    }

    public void releaseTickets(String reservationId, String bookingId) {
        String url = inventoryServiceBaseUrl + "/inventory/release";

        ReleaseInventoryRequest request = new ReleaseInventoryRequest(reservationId, bookingId);

        log.info("calling inventory service to release held tickets", Map.of(
                "targetService", "inventory-service",
                "bookingId", bookingId,
                "reservationId", reservationId
        ));

        try {
            restTemplate.postForEntity(url, request, Void.class);

            log.info("inventory release succeeded", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId,
                    "reservationId", reservationId
            ));
        } catch (RestClientException ex) {
            log.error("inventory release failed", Map.of(
                    "targetService", "inventory-service",
                    "bookingId", bookingId,
                    "reservationId", reservationId
            ), ex);

            throw new ExternalServiceException("Failed to release reserved tickets through Inventory Service", ex);
        }
    }
}