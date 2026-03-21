package com.phoenix.bookingservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.phoenix.bookingservice.client.EventServiceClient;
import com.phoenix.bookingservice.client.InventoryServiceClient;
import com.phoenix.bookingservice.client.PaymentServiceClient;
import com.phoenix.bookingservice.client.dto.CreatePaymentResponse;
import com.phoenix.bookingservice.client.dto.HoldInventoryResponse;
import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.dto.PaymentCallbackRequest;
import com.phoenix.bookingservice.dto.StartPaymentResponse;
import com.phoenix.bookingservice.entity.Booking;
import com.phoenix.bookingservice.entity.BookingStatus;
import com.phoenix.bookingservice.entity.PaymentStatus;
import com.phoenix.bookingservice.repository.BookingRepository;
import com.phoenix.bookingservice.service.BookingServiceImpl;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking baseBooking;

    @BeforeEach
    void setUp() {
        baseBooking = Booking.builder()
                .id("mongo-1")
                .bookingId("BKG-ABC1234567")
                .eventId("EVT-1001")
                .customerName("Christy Kingsley")
                .customerEmail("christy@example.com")
                .ticketType("VIP")
                .quantity(2)
                .totalAmount(new BigDecimal("5000.00"))
                .inventoryReservationId("RES-001")
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createBooking_shouldValidateHoldAndPersistBooking() {
        CreateBookingRequest request = new CreateBookingRequest(
                "EVT-1001",
                "Christy Kingsley",
                "christy@example.com",
                "VIP",
                2,
                new BigDecimal("5000.00")
        );

        when(bookingRepository.existsByBookingId(anyString())).thenReturn(false);
        when(inventoryServiceClient.holdTickets(anyString(), eq("EVT-1001"), eq("VIP"), eq(2)))
                .thenReturn(new HoldInventoryResponse("RES-001", "HELD", Instant.now().plusSeconds(300)));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId("mongo-1");
            return booking;
        });

        BookingResponse response = bookingService.createBooking(request);

        verify(eventServiceClient).verifyEventExistsAndIsActive("EVT-1001");
        verify(inventoryServiceClient).checkAvailability("EVT-1001", "VIP", 2);
        verify(inventoryServiceClient).holdTickets(anyString(), eq("EVT-1001"), eq("VIP"), eq(2));

        assertNotNull(response.getBookingId());
        assertEquals(BookingStatus.PENDING, response.getBookingStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals("EVT-1001", response.getEventId());
    }

    @Test
    void startPayment_shouldCreatePaymentAndUpdateBooking() {
        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(paymentServiceClient.createPayment(any(Booking.class)))
                .thenReturn(new CreatePaymentResponse("PAY-123456", "https://payment.example/checkout/123", "PENDING"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StartPaymentResponse response = bookingService.startPayment("BKG-ABC1234567");

        verify(paymentServiceClient).createPayment(any(Booking.class));
        assertEquals("PAY-123456", response.getPaymentReferenceId());
        assertEquals(BookingStatus.AWAITING_PAYMENT, response.getBookingStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
    }

    @Test
    void handlePaymentCallback_success_shouldConfirmInventoryAndConfirmBooking() {
        baseBooking.setPaymentReferenceId("PAY-123456");
        baseBooking.setBookingStatus(BookingStatus.AWAITING_PAYMENT);
        baseBooking.setPaymentStatus(PaymentStatus.PENDING);

        PaymentCallbackRequest request = new PaymentCallbackRequest(
                "BKG-ABC1234567",
                "PAY-123456",
                "SUCCESS",
                "TXN-0001"
        );

        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.handlePaymentCallback(request);

        verify(inventoryServiceClient).confirmTickets("RES-001", "BKG-ABC1234567");
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals("TXN-0001", response.getPaymentTransactionId());
    }

    @Test
    void cancelBooking_shouldReleaseInventoryAndMarkCancelled() {
        baseBooking.setBookingStatus(BookingStatus.AWAITING_PAYMENT);
        baseBooking.setPaymentStatus(PaymentStatus.PENDING);

        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking("BKG-ABC1234567");

        verify(inventoryServiceClient).releaseTickets("RES-001", "BKG-ABC1234567");
        assertEquals(BookingStatus.CANCELLED, response.getBookingStatus());
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
    }

    @Test
    void expireBooking_shouldReleaseInventoryAndMarkExpired() {
        baseBooking.setBookingStatus(BookingStatus.PENDING);
        baseBooking.setPaymentStatus(PaymentStatus.PENDING);

        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.expireBooking("BKG-ABC1234567");

        verify(inventoryServiceClient).releaseTickets("RES-001", "BKG-ABC1234567");
        assertEquals(BookingStatus.EXPIRED, response.getBookingStatus());
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
    }
}