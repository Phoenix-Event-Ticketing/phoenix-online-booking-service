package com.phoenix.bookingservice.client;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import com.phoenix.bookingservice.security.InternalServiceTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryServiceClient {

    private static final StructuredLogger log = StructuredLogger.getLogger(InventoryServiceClient.class);
    private static final String TARGET_SERVICE = "targetService";
    private static final String INVENTORY_SERVICE = "inventory-service";

    private final RestTemplate restTemplate;
    private final InternalServiceTokenProvider internalServiceTokenProvider;

    @Value("${services.inventory.base-url}")
    private String inventoryServiceBaseUrl;

    public void checkAvailability(String eventId, String ticketType, Integer quantity) {
        String url = inventoryServiceBaseUrl
                + "/inventory/event/" + eventId + "/availability";

        log.info("calling inventory service for availability check", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));

        try {
            ResponseEntity<InventoryAvailabilityResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(buildAuthHeaders()),
                    InventoryAvailabilityResponse.class
            );

            InventoryAvailabilityResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new ExternalServiceException("Invalid response received from Inventory Service");
            }

            int availableQuantity = Optional.ofNullable(body.getItems())
                    .orElseGet(java.util.List::of)
                    .stream()
                    .filter(item -> ticketType.equalsIgnoreCase(item.getTicketType()))
                    .map(InventoryAvailabilityResponse.AvailabilityItem::getAvailableQuantity)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(0);

            if (availableQuantity < quantity) {
                throw new BusinessValidationException("Requested ticket quantity is not available");
            }

            log.info("inventory availability check succeeded", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessValidationException("Ticket inventory not found for the selected event and ticket type");
            }

            log.error("inventory service returned an error", Map.of(TARGET_SERVICE, INVENTORY_SERVICE), ex);

            throw new ExternalServiceException("Inventory Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("inventory service communication failed", Map.of(TARGET_SERVICE, INVENTORY_SERVICE), ex);

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

        log.info("calling inventory service to hold tickets", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));

        try {
            ResponseEntity<HoldInventoryResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            new HttpEntity<>(request, buildAuthHeaders()),
                            HoldInventoryResponse.class
                    );

            HoldInventoryResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.getBookingId() == null) {
                throw new ExternalServiceException("Invalid hold response received from Inventory Service");
            }

            log.info("inventory hold succeeded", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));

            return body;

        } catch (HttpStatusCodeException ex) {
            log.error("inventory hold failed with downstream error", Map.of(TARGET_SERVICE, INVENTORY_SERVICE), ex);

            throw new ExternalServiceException("Inventory hold request failed: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("inventory hold communication failed", Map.of(TARGET_SERVICE, INVENTORY_SERVICE), ex);

            throw new ExternalServiceException("Failed to reserve tickets through Inventory Service", ex);
        }
    }

    public void confirmTickets(String bookingId) {
        String url = inventoryServiceBaseUrl + "/inventory/confirm";

        ConfirmInventoryRequest request = new ConfirmInventoryRequest(bookingId);

        log.info("calling inventory service to confirm held tickets", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, buildAuthHeaders()),
                    Void.class
            );

            log.info("inventory confirmation succeeded", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));
        } catch (RestClientException ex) {
            log.error("inventory confirmation failed", Map.of(TARGET_SERVICE, INVENTORY_SERVICE), ex);

            throw new ExternalServiceException("Failed to confirm reserved tickets through Inventory Service", ex);
        }
    }

    public void releaseTickets(String bookingId) {
        String url = inventoryServiceBaseUrl + "/inventory/release";

        ReleaseInventoryRequest request = new ReleaseInventoryRequest(bookingId);

        log.info("calling inventory service to release held tickets", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, buildAuthHeaders()),
                    Void.class
            );

            log.info("inventory release succeeded", Map.of(TARGET_SERVICE, INVENTORY_SERVICE));
        } catch (RestClientException ex) {
            log.error("inventory release failed", Map.of(TARGET_SERVICE, INVENTORY_SERVICE), ex);

            throw new ExternalServiceException("Failed to release reserved tickets through Inventory Service", ex);
        }
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(internalServiceTokenProvider.createServiceToken());
        return headers;
    }
}