package com.phoenix.bookingservice.unit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.client.InventoryServiceClient;
import com.phoenix.bookingservice.client.dto.ConfirmInventoryRequest;
import com.phoenix.bookingservice.client.dto.HoldInventoryResponse;
import com.phoenix.bookingservice.client.dto.InventoryAvailabilityResponse;
import com.phoenix.bookingservice.client.dto.ReleaseInventoryRequest;
import com.phoenix.bookingservice.exception.BusinessValidationException;
import com.phoenix.bookingservice.security.InternalServiceTokenProvider;

@ExtendWith(MockitoExtension.class)
class InventoryServiceClientTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InternalServiceTokenProvider internalServiceTokenProvider;

    private InventoryServiceClient inventoryServiceClient;

    @BeforeEach
    void setUp() {
        inventoryServiceClient = new InventoryServiceClient(restTemplate, internalServiceTokenProvider);
        ReflectionTestUtils.setField(inventoryServiceClient, "inventoryServiceBaseUrl", "http://localhost:8082");
        when(internalServiceTokenProvider.createServiceToken()).thenReturn("svc-token");
    }

    @Test
    void checkAvailability_shouldPassWhenTicketTypeHasEnoughQuantity() {
        InventoryAvailabilityResponse responseBody = new InventoryAvailabilityResponse(
                "EVT-1001",
                List.of(
                        new InventoryAvailabilityResponse.AvailabilityItem(
                                "inv-1",
                                "VIP",
                                new BigDecimal("2500.00"),
                                20,
                                2,
                                10,
                                8
                        )
                )
        );

        when(restTemplate.exchange(
                eq("http://localhost:8082/inventory/event/EVT-1001/availability"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(InventoryAvailabilityResponse.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        assertDoesNotThrow(() -> inventoryServiceClient.checkAvailability("EVT-1001", "VIP", 2));
    }

    @Test
    void checkAvailability_shouldRejectWhenTicketTypeMissingOrInsufficient() {
        InventoryAvailabilityResponse responseBody = new InventoryAvailabilityResponse(
                "EVT-1001",
                List.of(
                        new InventoryAvailabilityResponse.AvailabilityItem(
                                "inv-2",
                                "STANDARD",
                                new BigDecimal("1500.00"),
                                100,
                                10,
                                85,
                                5
                        )
                )
        );

        when(restTemplate.exchange(
                eq("http://localhost:8082/inventory/event/EVT-1001/availability"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(InventoryAvailabilityResponse.class)
        )).thenReturn(ResponseEntity.ok(responseBody));

        assertThrows(
                BusinessValidationException.class,
                () -> inventoryServiceClient.checkAvailability("EVT-1001", "VIP", 1)
        );
    }

    @Test
    void holdTickets_shouldRequireBookingIdInResponse() {
        HoldInventoryResponse responseBody = new HoldInventoryResponse(
                "BKG-ABC1234567",
                "HELD",
                Instant.now().plusSeconds(300)
        );

        when(restTemplate.exchange(
                eq("http://localhost:8082/inventory/hold"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(HoldInventoryResponse.class)
        )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        HoldInventoryResponse response = inventoryServiceClient.holdTickets("BKG-ABC1234567", "EVT-1001", "VIP", 2);

        assertEquals("BKG-ABC1234567", response.getBookingId());
        assertEquals("HELD", response.getHoldStatus());
    }

    @Test
    void confirmAndRelease_shouldSendBookingIdPayload() {
        when(restTemplate.exchange(
                eq("http://localhost:8082/inventory/confirm"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        when(restTemplate.exchange(
                eq("http://localhost:8082/inventory/release"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Void.class)
        )).thenReturn(ResponseEntity.ok().build());

        inventoryServiceClient.confirmTickets("BKG-ABC1234567");
        inventoryServiceClient.releaseTickets("BKG-ABC1234567");

        ArgumentCaptor<HttpEntity> requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq("http://localhost:8082/inventory/confirm"),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(Void.class)
        );
        Object confirmBody = requestCaptor.getValue().getBody();
        assertEquals("BKG-ABC1234567", ((ConfirmInventoryRequest) confirmBody).getBookingId());

        verify(restTemplate).exchange(
                eq("http://localhost:8082/inventory/release"),
                eq(HttpMethod.POST),
                requestCaptor.capture(),
                eq(Void.class)
        );
        Object releaseBody = requestCaptor.getValue().getBody();
        assertEquals("BKG-ABC1234567", ((ReleaseInventoryRequest) releaseBody).getBookingId());
    }
}
