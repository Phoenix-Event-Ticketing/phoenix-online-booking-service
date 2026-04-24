package com.phoenix.bookingservice.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.phoenix.bookingservice.client.dto.CreatePaymentRequest;
import com.phoenix.bookingservice.client.dto.CreatePaymentResponse;
import com.phoenix.bookingservice.entity.Booking;
import com.phoenix.bookingservice.exception.ExternalServiceException;
import com.phoenix.bookingservice.logging.StructuredLogger;
import com.phoenix.bookingservice.security.InternalServicePermissions;
import com.phoenix.bookingservice.security.InternalServiceTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

    private static final StructuredLogger log = StructuredLogger.getLogger(PaymentServiceClient.class);
    private static final String TARGET_SERVICE = "targetService";
    private static final String PAYMENT_SERVICE = "payment-service";

    private final RestTemplate restTemplate;
    private final InternalServiceTokenProvider internalServiceTokenProvider;

    @Value("${services.payment.base-url}")
    private String paymentServiceBaseUrl;

    @Value("${services.booking.callback-base-url}")
    private String bookingServiceCallbackBaseUrl;

    public CreatePaymentResponse createPayment(Booking booking, String paymentMethod) {
        String url = paymentServiceBaseUrl + "/internal/payments";
        String resolvedPaymentMethod = normalizePaymentMethod(paymentMethod);

        CreatePaymentRequest request = new CreatePaymentRequest(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getTotalAmount(),
                "LKR",
                resolvedPaymentMethod,
                booking.getCustomerEmail(),
                bookingServiceCallbackBaseUrl + "/bookings/payment-callback",
                "Ticket booking payment for " + booking.getBookingId()
        );

        log.info("calling payment service to create payment", Map.of(TARGET_SERVICE, PAYMENT_SERVICE));

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(internalServiceTokenProvider.createServiceToken());
            headers.set(InternalServicePermissions.HEADER_SERVICE_ID, "booking-service");
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class
            );
            CreatePaymentResponse body = normalizeCreatePaymentResponse(response.getBody());

            if (!response.getStatusCode().is2xxSuccessful()
                    || body == null
                    || body.getPaymentReferenceId() == null
                    || body.getPaymentReferenceId().isBlank()) {
                throw new ExternalServiceException("Invalid response received from Payment Service");
            }

            log.info("payment service create payment succeeded", Map.of(TARGET_SERVICE, PAYMENT_SERVICE));

            return body;

        } catch (HttpStatusCodeException ex) {
            log.error("payment service returned an error", Map.of(TARGET_SERVICE, PAYMENT_SERVICE), ex);

            throw new ExternalServiceException("Payment Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("payment service communication failed", Map.of(TARGET_SERVICE, PAYMENT_SERVICE), ex);

            throw new ExternalServiceException("Failed to communicate with Payment Service", ex);
        }
    }

    private CreatePaymentResponse normalizeCreatePaymentResponse(Map<?, ?> payload) {
        if (payload == null) {
            return null;
        }
        Map<?, ?> data = payload;
        Object nestedData = payload.get("data");
        if (nestedData instanceof Map<?, ?> nested) {
            data = nested;
        }

        String paymentReferenceId = firstNonBlank(
                asString(data.get("paymentReferenceId")),
                asString(data.get("paymentId")),
                asString(data.get("id"))
        );
        String status = asString(data.get("status"));
        String paymentId = firstNonBlank(asString(data.get("paymentId")), asString(data.get("id")));
        String id = asString(data.get("id"));

        return new CreatePaymentResponse(paymentReferenceId, status, paymentId, id);
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toString().trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return "CARD";
        }
        String normalized = paymentMethod.trim().toUpperCase();
        return switch (normalized) {
            case "CARD", "BANK_TRANSFER", "WALLET" -> normalized;
            default -> "CARD";
        };
    }
}