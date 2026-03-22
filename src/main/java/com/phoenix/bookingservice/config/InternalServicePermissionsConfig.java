package com.phoenix.bookingservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.phoenix.bookingservice.security.InternalServicePermissions;

@Configuration
public class InternalServicePermissionsConfig {

    private static final String DEV_FALLBACK_JSON =
            "{\"payment-service\":[\"PAYMENT_CALLBACK\"],"
                    + "\"inventory-service\":[\"EXPIRE_BOOKING\"]}";

    @Bean
    public InternalServicePermissions internalServicePermissions(Environment env) {
        String json = env.getProperty("security.internal.service-permissions-json");
        if (json == null || json.isBlank()) {
            json = env.getProperty("INTERNAL_SERVICE_PERMISSIONS_JSON");
        }
        if (json == null || json.isBlank()) {
            json = DEV_FALLBACK_JSON;
        }
        return InternalServicePermissions.parse(json);
    }
}
