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
