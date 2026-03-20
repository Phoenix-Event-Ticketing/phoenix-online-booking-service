package com.phoenix.bookingservice.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.client.dto.EventSummaryResponse;
import com.phoenix.bookingservice.exception.BusinessValidationException;
import com.phoenix.bookingservice.exception.ExternalServiceException;
import com.phoenix.bookingservice.logging.StructuredLogger;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceClient {

    private static final StructuredLogger log = StructuredLogger.getLogger(EventServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.event.base-url}")
    private String eventServiceBaseUrl;

    public void verifyEventExistsAndIsActive(String eventId) {
        String url = eventServiceBaseUrl + "/events/" + eventId;

        log.info("calling event service for event validation", Map.of(
                "targetService", "event-service",
                "eventId", eventId
        ));

        try {
            ResponseEntity<EventSummaryResponse> response =
                    restTemplate.getForEntity(url, EventSummaryResponse.class);

            EventSummaryResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful() || body == null) {
                throw new ExternalServiceException("Invalid response received from Event Service");
            }

            if (Boolean.FALSE.equals(body.getActive())) {
                throw new BusinessValidationException("Selected event is not active");
            }

            log.info("event validation succeeded", Map.of(
                    "targetService", "event-service",
                    "eventId", eventId
            ));

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessValidationException("Event not found for ID: " + eventId);
            }

            log.error("event service returned an error", Map.of(
                    "targetService", "event-service",
                    "eventId", eventId,
                    "statusCode", ex.getStatusCode().value()
            ), ex);

            throw new ExternalServiceException("Event Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("event service communication failed", Map.of(
                    "targetService", "event-service",
                    "eventId", eventId
            ), ex);

            throw new ExternalServiceException("Failed to communicate with Event Service", ex);
        }
    }
}