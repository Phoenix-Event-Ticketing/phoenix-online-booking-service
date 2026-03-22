package com.phoenix.bookingservice.logging;

import org.slf4j.MDC;

public final class RequestContext {

    public static final String REQUEST_ID = "request_id";
    public static final String TRACE_ID = "trace_id";
    public static final String USER_ID = "user_id";
    public static final String OPERATION = "operation";
    public static final String HTTP_METHOD = "http_method";
    public static final String HTTP_PATH = "http_path";
    public static final String HTTP_ROUTE = "http_route";
    public static final String HTTP_STATUS_CODE = "http_status_code";
    public static final String HTTP_RESPONSE_TIME_MS = "http_response_time_ms";

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

    public static void setHttpMethod(String method) {
        MDC.put(HTTP_METHOD, method);
    }

    public static void setHttpPath(String path) {
        MDC.put(HTTP_PATH, path);
    }

    public static void setHttpRoute(String route) {
        MDC.put(HTTP_ROUTE, route);
    }

    public static void setHttpStatusCode(Integer statusCode) {
        if (statusCode != null) {
            MDC.put(HTTP_STATUS_CODE, String.valueOf(statusCode));
        } else {
            MDC.remove(HTTP_STATUS_CODE);
        }
    }

    public static void setHttpResponseTimeMs(Long responseTimeMs) {
        if (responseTimeMs != null) {
            MDC.put(HTTP_RESPONSE_TIME_MS, String.valueOf(responseTimeMs));
        } else {
            MDC.remove(HTTP_RESPONSE_TIME_MS);
        }
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

    public static String getHttpMethod() {
        return MDC.get(HTTP_METHOD);
    }

    public static String getHttpPath() {
        return MDC.get(HTTP_PATH);
    }

    public static String getHttpRoute() {
        return MDC.get(HTTP_ROUTE);
    }

    public static Integer getHttpStatusCode() {
        String v = MDC.get(HTTP_STATUS_CODE);
        return v != null ? Integer.parseInt(v) : null;
    }

    public static Long getHttpResponseTimeMs() {
        String v = MDC.get(HTTP_RESPONSE_TIME_MS);
        return v != null ? Long.parseLong(v) : null;
    }

    public static void clear() {
        MDC.clear();
    }
}