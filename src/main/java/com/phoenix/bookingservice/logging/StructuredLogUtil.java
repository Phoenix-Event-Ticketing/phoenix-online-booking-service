package com.phoenix.bookingservice.logging;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class StructuredLogUtil {

    private static final String CONTROL_CHARS_PATTERN = "[\\n\\r\\u0000-\\u001F\\u007F]";

    private StructuredLogUtil() {
    }

    /**
     * Sanitizes user-controlled input for safe logging to prevent log injection (S5145).
     * Removes newlines, carriage returns, and other control characters.
     */
    public static String sanitizeForLog(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.replaceAll(CONTROL_CHARS_PATTERN, " ");
    }

    /**
     * Derives a route pattern from path for logging (e.g. /bookings/BKG-123 -> /bookings/:id).
     */
    public static String deriveRoute(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String result = path.replaceAll("/bookings/customer/[^/]+", "/bookings/customer/:email");
        return result.replaceAll("/bookings/[^/]+(/|$)", "/bookings/:id$1");
    }

    /**
     * Builds log JSON with level, event, message, and http block from RequestContext.
     */
    public static String toJson(String level, String event, String message) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("timestamp", Instant.now().toString());
        log.put("level", level);
        log.put("service", "phoenix-online-booking-service");
        log.put("environment", System.getenv().getOrDefault("APP_ENV", "dev"));
        log.put("event", event);
        log.put("request_id", RequestContext.getRequestId());
        log.put("trace_id", RequestContext.getTraceId());
        log.put("user_id", RequestContext.getUserId());
        log.put("operation", RequestContext.getOperation());
        log.put("message", message);
        log.put("http", buildHttpBlock());

        return mapToJson(log);
    }

    private static Map<String, Object> buildHttpBlock() {
        Map<String, Object> http = new LinkedHashMap<>();
        String method = RequestContext.getHttpMethod();
        String path = RequestContext.getHttpPath();
        String route = RequestContext.getHttpRoute();
        Integer statusCode = RequestContext.getHttpStatusCode();
        Long responseTimeMs = RequestContext.getHttpResponseTimeMs();

        if (method != null) {
            http.put("method", method);
        }
        if (path != null) {
            http.put("path", sanitizeForLog(path));
        }
        if (route != null) {
            http.put("route", route);
        }
        if (statusCode != null) {
            http.put("status_code", statusCode);
        }
        if (responseTimeMs != null) {
            http.put("response_time_ms", responseTimeMs);
        }
        return http;
    }

    private static String mapToJson(Map<String, Object> map) {
        return map.entrySet()
                .stream()
                .map(entry -> "\"" + escape(entry.getKey()) + "\":" + valueToJson(entry.getValue()))
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String valueToJson(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof String str) {
            return "\"" + escape(sanitizeForLog(str)) + "\"";
        }

        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }

        if (value instanceof Map<?, ?> nestedMap) {
            Map<String, Object> converted = new LinkedHashMap<>();
            nestedMap.forEach((k, v) -> converted.put(String.valueOf(k), v));
            return mapToJson(converted);
        }

        return "\"" + escape(sanitizeForLog(String.valueOf(value))) + "\"";
    }

    private static String escape(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}