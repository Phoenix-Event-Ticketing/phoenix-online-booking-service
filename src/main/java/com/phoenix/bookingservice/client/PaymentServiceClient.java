package com.phoenix.bookingservice.client;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceClient {

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

            return body;

        } catch (HttpStatusCodeException ex) {
            throw new ExternalServiceException("Payment Service returned an error: " + ex.getStatusCode(), ex);
        } catch (RestClientException ex) {
            throw new ExternalServiceException("Failed to communicate with Payment Service", ex);
        }
    }
}