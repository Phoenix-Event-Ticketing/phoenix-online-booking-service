package com.phoenix.bookingservice.security;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    @Value("${security.internal.service-token}")
    private String internalServiceToken;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        boolean isPaymentCallback = "POST".equalsIgnoreCase(method)
                && "/bookings/payment-callback".equals(requestUri);

        boolean isExpireBooking = "POST".equalsIgnoreCase(method)
                && requestUri.matches("^/bookings/[^/]+/expire$");

        if (!isPaymentCallback && !isExpireBooking) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedToken = request.getHeader("X-Internal-Service-Token");

        if (providedToken == null || !providedToken.equals(internalServiceToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Invalid internal service token"
            ));
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "internal-booking-lifecycle-service",
                        null,
                        List.of(new SimpleGrantedAuthority(BookingPermissions.INTERNAL_SERVICE))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}