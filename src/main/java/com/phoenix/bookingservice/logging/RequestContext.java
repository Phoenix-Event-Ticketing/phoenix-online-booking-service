package com.phoenix.bookingservice.logging;

import org.slf4j.MDC;

public final class RequestContext {

    public static final String REQUEST_ID = "request_id";
    public static final String TRACE_ID = "trace_id";
    public static final String USER_ID = "user_id";
    public static final String OPERATION = "operation";

    private RequestContext() {
    }

    public static void setRequestId(String requestId) {
        MDC.put(REQUEST_ID, requestId);
    }

    public static void setTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void setUserId(String userId) {
        MDC.put(USER_ID, userId);
    }

    public static void setOperation(String operation) {
        MDC.put(OPERATION, operation);
    }

    public static String getRequestId() {
        return MDC.get(REQUEST_ID);
    }

    public static String getTraceId() {
        return MDC.get(TRACE_ID);
    }

    public static String getUserId() {
        return MDC.get(USER_ID);
    }

    public static String getOperation() {
        return MDC.get(OPERATION);
    }

    public static void clear() {
        MDC.clear();
    }
}