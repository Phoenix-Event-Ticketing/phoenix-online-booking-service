package com.phoenix.bookingservice.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parsed JSON mapping service id → list of allowed permissions.
 * Used for internal service authorization (no secrets).
 */
public final class InternalServicePermissions {

    public static final String HEADER_SERVICE_ID = "X-Internal-Service-Id";

    /** Permission for POST /bookings/payment-callback (payment service). */
    public static final String PAYMENT_CALLBACK = "PAYMENT_CALLBACK";

    /** Permission for POST /bookings/{id}/expire (inventory/scheduler). */
    public static final String EXPIRE_BOOKING = "EXPIRE_BOOKING";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Set<String>> serviceIdToPermissions;

    private InternalServicePermissions(Map<String, Set<String>> serviceIdToPermissions) {
        this.serviceIdToPermissions = serviceIdToPermissions;
    }

    public static InternalServicePermissions parse(String json) {
        if (json == null || json.isBlank()) {
            return new InternalServicePermissions(Map.of());
        }
        try {
            Map<String, List<String>> raw = MAPPER.readValue(json, new TypeReference<>() {});
            if (raw == null || raw.isEmpty()) {
                return new InternalServicePermissions(Map.of());
            }
            Map<String, Set<String>> normalized = new java.util.LinkedHashMap<>();
            for (Map.Entry<String, List<String>> e : raw.entrySet()) {
                if (e.getKey() != null && !e.getKey().isBlank() && e.getValue() != null) {
                    Set<String> perms = e.getValue().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(p -> !p.isBlank())
                            .collect(Collectors.toUnmodifiableSet());
                    if (!perms.isEmpty()) {
                        normalized.put(e.getKey().trim(), perms);
                    }
                }
            }
            return new InternalServicePermissions(Collections.unmodifiableMap(normalized));
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "security.internal.service-permissions-json must be valid JSON "
                            + "{\"service-id\":[\"PERM1\",\"PERM2\"],...}",
                    ex);
        }
    }

    public boolean isEmpty() {
        return serviceIdToPermissions.isEmpty();
    }

    public boolean hasPermission(String serviceId, String requiredPermission) {
        if (serviceId == null || serviceId.isBlank() || requiredPermission == null || requiredPermission.isBlank()) {
            return false;
        }
        Set<String> perms = serviceIdToPermissions.get(serviceId.trim());
        return perms != null && perms.contains(requiredPermission.trim());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalServicePermissions that = (InternalServicePermissions) o;
        return Objects.equals(serviceIdToPermissions, that.serviceIdToPermissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceIdToPermissions);
    }
}
