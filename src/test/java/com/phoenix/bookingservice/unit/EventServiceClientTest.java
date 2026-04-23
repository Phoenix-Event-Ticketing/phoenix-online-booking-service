package com.phoenix.bookingservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.client.EventServiceClient;
import com.phoenix.bookingservice.client.dto.EventSummaryResponse;
import com.phoenix.bookingservice.exception.BusinessValidationException;
import com.phoenix.bookingservice.exception.ExternalServiceException;

@ExtendWith(MockitoExtension.class)
class EventServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    private EventServiceClient eventServiceClient;

    @BeforeEach
    void setUp() {
        eventServiceClient = new EventServiceClient(restTemplate);
        ReflectionTestUtils.setField(eventServiceClient, "eventServiceBaseUrl", "http://localhost:8081");
    }

    @Test
    void verifyEventExistsAndIsActive_shouldPassWhenStatusIsPublished() {
        EventSummaryResponse responseBody = new EventSummaryResponse("evt_1", "Music Fest", "PUBLISHED");
        when(restTemplate.getForEntity("http://localhost:8081/events/evt_1", EventSummaryResponse.class))
                .thenReturn(ResponseEntity.ok(responseBody));

        assertDoesNotThrow(() -> eventServiceClient.verifyEventExistsAndIsActive("evt_1"));
    }

    @Test
    void verifyEventExistsAndIsActive_shouldRejectWhenStatusIsDraft() {
        EventSummaryResponse responseBody = new EventSummaryResponse("evt_1", "Music Fest", "DRAFT");
        when(restTemplate.getForEntity("http://localhost:8081/events/evt_1", EventSummaryResponse.class))
                .thenReturn(ResponseEntity.ok(responseBody));

        assertThrows(
                BusinessValidationException.class,
                () -> eventServiceClient.verifyEventExistsAndIsActive("evt_1")
        );
    }

    @Test
    void verifyEventExistsAndIsActive_shouldRejectWhenStatusIsCancelled() {
        EventSummaryResponse responseBody = new EventSummaryResponse("evt_1", "Music Fest", "CANCELLED");
        when(restTemplate.getForEntity("http://localhost:8081/events/evt_1", EventSummaryResponse.class))
                .thenReturn(ResponseEntity.ok(responseBody));

        assertThrows(
                BusinessValidationException.class,
                () -> eventServiceClient.verifyEventExistsAndIsActive("evt_1")
        );
    }

    @Test
    void verifyEventExistsAndIsActive_shouldMap404ToBusinessValidationException() {
        HttpClientErrorException notFound = HttpClientErrorException.create(
                HttpStatus.NOT_FOUND,
                "Not Found",
                HttpHeaders.EMPTY,
                null,
                null
        );
        when(restTemplate.getForEntity("http://localhost:8081/events/evt_404", EventSummaryResponse.class))
                .thenThrow(notFound);

        assertThrows(
                BusinessValidationException.class,
                () -> eventServiceClient.verifyEventExistsAndIsActive("evt_404")
        );
    }

    @Test
    void verifyEventExistsAndIsActive_shouldThrowExternalServiceExceptionForMalformedResponse() {
        EventSummaryResponse responseBody = new EventSummaryResponse("evt_1", "Music Fest", null);
        when(restTemplate.getForEntity("http://localhost:8081/events/evt_1", EventSummaryResponse.class))
                .thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        assertThrows(
                ExternalServiceException.class,
                () -> eventServiceClient.verifyEventExistsAndIsActive("evt_1")
        );
    }
}
