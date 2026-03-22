package com.phoenix.bookingservice.logging;

import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.phoenix.bookingservice.security.CustomUserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class CustomRequestContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CustomRequestContextFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long start = System.currentTimeMillis();

        String requestId = headerOrRandom(request.getHeader(REQUEST_ID_HEADER));
        String traceId = headerOrRandom(request.getHeader(TRACE_ID_HEADER));
        String path = request.getRequestURI();

        RequestContext.setRequestId(requestId);
        RequestContext.setTraceId(traceId);
        RequestContext.setOperation(request.getMethod());
        RequestContext.setHttpMethod(request.getMethod());
        RequestContext.setHttpPath(path);
        RequestContext.setHttpRoute(StructuredLogUtil.deriveRoute(path));

        attachAuthenticatedUserIfPresent();

        response.setHeader(REQUEST_ID_HEADER, requestId);
        response.setHeader(TRACE_ID_HEADER, traceId);

        log.info(StructuredLogUtil.toJson("INFO", "request_started", "request started"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;

            RequestContext.setHttpStatusCode(response.getStatus());
            RequestContext.setHttpResponseTimeMs(durationMs);

            log.info(StructuredLogUtil.toJson("INFO", "request_completed", "request completed"));

            RequestContext.clear();
        }
    }

    private void attachAuthenticatedUserIfPresent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            return;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            if (customUserPrincipal.getUserId() != null) {
                RequestContext.setUserId(customUserPrincipal.getUserId());
            }
        } else if (principal instanceof String principalText && !"anonymousUser".equals(principalText)) {
            RequestContext.setUserId(principalText);
        }
    }

    private String headerOrRandom(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}