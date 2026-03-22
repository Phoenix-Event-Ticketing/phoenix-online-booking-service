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

    public static String toJson(String message, Map<String, Object> metadata) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("timestamp", Instant.now().toString());
        log.put("service", "phoenix-online-booking-service");
        log.put("environment", System.getenv().getOrDefault("APP_ENV", "dev"));
        log.put("request_id", RequestContext.getRequestId());
        log.put("trace_id", RequestContext.getTraceId());
        log.put("user_id", RequestContext.getUserId());
        log.put("operation", RequestContext.getOperation());
        log.put("message", message);
        log.put("metadata", metadata == null ? Map.of() : metadata);

        return mapToJson(log);
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