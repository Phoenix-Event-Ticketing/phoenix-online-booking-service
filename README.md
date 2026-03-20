# Phoenix Online Booking Service

Online Booking Service for the Phoenix Online microservices application.

## Tech Stack

- Java 21
- Spring Boot
- MongoDB
- Docker
- Swagger / OpenAPI

## Responsibilities

- Create and manage bookings
- Coordinate with Event Service
- Communicate with Ticket Inventory Service
- Trigger Payment Service
- Handle payment callback updates

## Planned Endpoints

- POST /bookings
- GET /bookings/{bookingId}
- GET /bookings/customer/{email}
- PATCH /bookings/{bookingId}/cancel
- POST /bookings/{bookingId}/start-payment
- POST /bookings/payment-callback
- POST /bookings/{bookingId}/expire

## Security

- JWT-based authentication for normal Booking API requests
- Permission-based authorization using claims from JWT tokens
- Internal token protection for service-to-service payment callbacks
- Stateless Spring Security configuration
- Secrets supplied through environment variables

## Logging

- Structured JSON-style logs written to console
- request_id and trace_id support for request correlation
- operation and metadata fields for better troubleshooting
- outbound service-call logging for Event, Inventory, and Payment integrations
- no sensitive values such as secrets or JWT tokens should be logged

## Booking Lifecycle Actions

- PATCH /bookings/{bookingId}/cancel
- POST /bookings/{bookingId}/expire

### Lifecycle rules

- Bookings in PENDING or AWAITING_PAYMENT can be cancelled or expired
- Cancelling or expiring a booking releases any held inventory reservation
- Confirmed bookings are not cancelled or expired through this flow
- Expire endpoint is treated as an internal/system action
