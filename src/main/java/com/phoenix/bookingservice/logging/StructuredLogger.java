package com.phoenix.bookingservice.logging;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StructuredLogger {

    private final Logger logger;

    private StructuredLogger(Class<?> clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    public static StructuredLogger getLogger(Class<?> clazz) {
        return new StructuredLogger(clazz);
    }

    public void info(String message, Map<String, Object> metadata) {
        logger.info(StructuredLogUtil.toJson(message, metadata));
    }

    public void warn(String message, Map<String, Object> metadata) {
        logger.warn(StructuredLogUtil.toJson(message, metadata));
    }

    public void error(String message, Map<String, Object> metadata) {
        logger.error(StructuredLogUtil.toJson(message, metadata));
    }

    public void error(String message, Map<String, Object> metadata, Throwable ex) {
        logger.error(StructuredLogUtil.toJson(message, metadata), ex);
    }
}