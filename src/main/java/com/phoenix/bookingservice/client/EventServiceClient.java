package com.phoenix.bookingservice.client;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.event.base-url}")
    private String eventServiceBaseUrl;

    public void verifyEventExistsAndIsActive(String eventId) {
        String url = eventServiceBaseUrl + "/events/" + eventId;

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

        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessValidationException("Event not found for ID: " + eventId);
            }

            throw new ExternalServiceException("Event Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to communicate with Event Service", ex);
        }
    }
}