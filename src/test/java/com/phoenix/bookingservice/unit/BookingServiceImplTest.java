package com.phoenix.bookingservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
import com.phoenix.bookingservice.client.dto.InventoryAvailabilityResponse;
import com.phoenix.bookingservice.dto.BookingResponse;
import com.phoenix.bookingservice.dto.CreateBookingRequest;
import com.phoenix.bookingservice.dto.PaymentCallbackRequest;
import com.phoenix.bookingservice.dto.StartPaymentResponse;
import com.phoenix.bookingservice.dto.UpdateBookingRequest;
import com.phoenix.bookingservice.exception.BusinessValidationException;
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
                .userId("usr-001")
                .customerEmail("christy@example.com")
                .seat("A123")
                .ticketType("VIP")
                .quantity(2)
                .totalAmount(new BigDecimal("5000.00"))
                .inventoryReservationId("RES-001")
                .bookingStatus(BookingStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void getAllBookings_shouldReturnMappedResponses() {
        when(bookingRepository.findAll()).thenReturn(List.of(baseBooking));

        List<BookingResponse> responses = bookingService.getAllBookings();

        assertEquals(1, responses.size());
        assertEquals("BKG-ABC1234567", responses.getFirst().getBookingId());
    }

    @Test
    void updateBooking_shouldApplyProvidedFields() {
        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateBookingRequest request = new UpdateBookingRequest(
                null,
                "new@example.com",
                "B200",
                "usr-002",
                null,
                null,
                null
        );

        BookingResponse response = bookingService.updateBooking("BKG-ABC1234567", request);

        assertEquals("new@example.com", response.getCustomerEmail());
        assertEquals("B200", response.getSeat());
        assertEquals("usr-002", response.getUserId());
    }

    @Test
    void updateBooking_shouldRejectWhenNoFieldsProvided() {
        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));

        assertThrows(
                BusinessValidationException.class,
                () -> bookingService.updateBooking(
                        "BKG-ABC1234567",
                        new UpdateBookingRequest(null, null, null, null, null, null, null)
                )
        );
    }

    @Test
    void createBooking_shouldValidateHoldAndPersistBooking() {
        CreateBookingRequest request = new CreateBookingRequest(
                "EVT-1001",
                "christy@example.com",
                "VIP",
                2,
                new BigDecimal("5000.00"),
                "A123",
                "usr-001"
        );

        when(bookingRepository.existsByBookingId(anyString())).thenReturn(false);
        when(inventoryServiceClient.checkAvailability("EVT-1001", "VIP", 2))
                .thenReturn(new InventoryAvailabilityResponse.AvailabilityItem(
                        "inv-1",
                        "VIP",
                        new BigDecimal("2500.00"),
                        10,
                        0,
                        0,
                        10
                ));
        when(inventoryServiceClient.holdTickets(anyString(), eq("EVT-1001"), eq("VIP"), eq(2)))
                .thenReturn(new HoldInventoryResponse("BKG-ABC1234567", "HELD", Instant.now().plusSeconds(300)));
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
        assertEquals(BookingStatus.AWAITING_PAYMENT, response.getBookingStatus());
        assertEquals(PaymentStatus.PENDING, response.getPaymentStatus());
        assertEquals("EVT-1001", response.getEventId());
    }

    @Test
    void startPayment_shouldCreatePaymentAndUpdateBooking() {
        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(paymentServiceClient.createPayment(any(Booking.class)))
                .thenReturn(new CreatePaymentResponse("PAY-123456", "PENDING", "PAY-123456", "PAY-123456"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StartPaymentResponse response = bookingService.startPayment("BKG-ABC1234567", "CARD");

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

        verify(inventoryServiceClient).confirmTickets("BKG-ABC1234567");
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
        assertEquals("TXN-0001", response.getPaymentTransactionId());
    }

    @Test
    void handlePaymentCallback_duplicateSuccess_shouldNoop() {
        baseBooking.setPaymentReferenceId("PAY-123456");
        baseBooking.setBookingStatus(BookingStatus.CONFIRMED);
        baseBooking.setPaymentStatus(PaymentStatus.SUCCESS);

        PaymentCallbackRequest request = new PaymentCallbackRequest(
                "BKG-ABC1234567",
                "PAY-123456",
                "SUCCESS",
                "TXN-0001"
        );

        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));

        BookingResponse response = bookingService.handlePaymentCallback(request);

        verify(inventoryServiceClient, never()).confirmTickets(anyString());
        verify(bookingRepository, never()).save(any(Booking.class));
        assertEquals(BookingStatus.CONFIRMED, response.getBookingStatus());
        assertEquals(PaymentStatus.SUCCESS, response.getPaymentStatus());
    }

    @Test
    void cancelBooking_shouldReleaseInventoryAndMarkCancelled() {
        baseBooking.setBookingStatus(BookingStatus.AWAITING_PAYMENT);
        baseBooking.setPaymentStatus(PaymentStatus.PENDING);

        when(bookingRepository.findByBookingId("BKG-ABC1234567")).thenReturn(Optional.of(baseBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking("BKG-ABC1234567");

        verify(inventoryServiceClient).releaseTickets("BKG-ABC1234567");
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

        verify(inventoryServiceClient).releaseTickets("BKG-ABC1234567");
        assertEquals(BookingStatus.EXPIRED, response.getBookingStatus());
        assertEquals(PaymentStatus.FAILED, response.getPaymentStatus());
    }

    @Test
    void expireStalePendingBookings_shouldExpirePendingAndAwaitingPayment() {
        Booking pendingBooking = Booking.builder()
                .bookingId("BKG-PENDING-001")
                .bookingStatus(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        Booking awaitingBooking = Booking.builder()
                .bookingId("BKG-AWAIT-001")
                .bookingStatus(BookingStatus.AWAITING_PAYMENT)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(bookingRepository.findByBookingStatusInAndPaymentStatusAndUpdatedAtBefore(
                any(),
                eq(PaymentStatus.PENDING),
                any(Instant.class)
        )).thenReturn(List.of(pendingBooking, awaitingBooking));

        when(bookingRepository.findByBookingId("BKG-PENDING-001")).thenReturn(Optional.of(pendingBooking));
        when(bookingRepository.findByBookingId("BKG-AWAIT-001")).thenReturn(Optional.of(awaitingBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int expired = bookingService.expireStalePendingBookings();

        assertEquals(2, expired);
        verify(inventoryServiceClient).releaseTickets("BKG-PENDING-001");
        verify(inventoryServiceClient).releaseTickets("BKG-AWAIT-001");
    }
}