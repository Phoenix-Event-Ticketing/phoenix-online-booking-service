package com.phoenix.bookingservice.security;

import java.io.IOException;
import java.util.List;

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

    private final InternalServicePermissions internalPermissions;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InternalServiceAuthFilter(InternalServicePermissions internalPermissions) {
        this.internalPermissions = internalPermissions;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        String requiredPermission = resolveRequiredPermission(method, requestUri);
        if (requiredPermission == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String serviceId = request.getHeader(InternalServicePermissions.HEADER_SERVICE_ID);

        if (internalPermissions.isEmpty()
                || !internalPermissions.hasPermission(serviceId, requiredPermission)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                    "status", 403,
                    "error", "Forbidden",
                    "message", "Service not authorized for " + requiredPermission
            ));
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "internal-" + (serviceId != null ? serviceId.trim() : "unknown"),
                        null,
                        List.of(new SimpleGrantedAuthority(BookingPermissions.INTERNAL_SERVICE))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private static String resolveRequiredPermission(String method, String requestUri) {
        if (!"POST".equalsIgnoreCase(method)) {
            return null;
        }
        if ("/bookings/payment-callback".equals(requestUri)) {
            return InternalServicePermissions.PAYMENT_CALLBACK;
        }
        if (requestUri != null && requestUri.matches("^/bookings/[^/]+/expire$")) {
            return InternalServicePermissions.EXPIRE_BOOKING;
        }
        return null;
    }
}