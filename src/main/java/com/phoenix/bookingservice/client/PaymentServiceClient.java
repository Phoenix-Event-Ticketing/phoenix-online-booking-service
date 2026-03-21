package com.phoenix.bookingservice.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

    private static final StructuredLogger log = StructuredLogger.getLogger(PaymentServiceClient.class);

    private final RestTemplate restTemplate;

    @Value("${services.payment.base-url}")
    private String paymentServiceBaseUrl;

    @Value("${services.booking.callback-base-url}")
    private String bookingServiceCallbackBaseUrl;

    public CreatePaymentResponse createPayment(Booking booking) {
        String url = paymentServiceBaseUrl + "/payments";

        CreatePaymentRequest request = new CreatePaymentRequest(
                booking.getBookingId(),
                booking.getTotalAmount(),
                booking.getCustomerEmail(),
                bookingServiceCallbackBaseUrl + "/bookings/payment-callback",
                "Ticket booking payment for " + booking.getBookingId()
        );

        log.info("calling payment service to create payment", Map.of(
                "targetService", "payment-service",
                "bookingId", booking.getBookingId(),
                "amount", booking.getTotalAmount()
        ));

        try {
            ResponseEntity<CreatePaymentResponse> response =
                    restTemplate.postForEntity(url, request, CreatePaymentResponse.class);

            CreatePaymentResponse body = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()
                    || body == null
                    || body.getPaymentReferenceId() == null
                    || body.getPaymentReferenceId().isBlank()) {
                throw new ExternalServiceException("Invalid response received from Payment Service");
            }

            log.info("payment service create payment succeeded", Map.of(
                    "targetService", "payment-service",
                    "bookingId", booking.getBookingId(),
                    "paymentReferenceId", body.getPaymentReferenceId()
            ));

            return body;

        } catch (HttpStatusCodeException ex) {
            log.error("payment service returned an error", Map.of(
                    "targetService", "payment-service",
                    "bookingId", booking.getBookingId(),
                    "statusCode", ex.getStatusCode().value()
            ), ex);

            throw new ExternalServiceException("Payment Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("payment service communication failed", Map.of(
                    "targetService", "payment-service",
                    "bookingId", booking.getBookingId()
            ), ex);

            throw new ExternalServiceException("Failed to communicate with Payment Service", ex);
        }
    }
}