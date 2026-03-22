package com.phoenix.bookingservice.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InternalServicePermissionsTest {

    @Test
    void parse_validJson_mapsIdsToPermissions() {
        InternalServicePermissions perms = InternalServicePermissions.parse(
                "{\"booking-service\":[\"RESERVE_TICKET\"],\"event-service\":[\"CREATE_TICKET_TYPE\",\"UPDATE_TICKET_INVENTORY\"]}");

        assertThat(perms.isEmpty()).isFalse();
        assertThat(perms.hasPermission("booking-service", "RESERVE_TICKET")).isTrue();
        assertThat(perms.hasPermission("event-service", "CREATE_TICKET_TYPE")).isTrue();
        assertThat(perms.hasPermission("event-service", "UPDATE_TICKET_INVENTORY")).isTrue();
        assertThat(perms.hasPermission("booking-service", "CREATE_TICKET_TYPE")).isFalse();
        assertThat(perms.hasPermission("unknown", "RESERVE_TICKET")).isFalse();
    }

    @Test
    void parse_bookingServiceFormat() {
        InternalServicePermissions perms = InternalServicePermissions.parse(
                "{\"payment-service\":[\"PAYMENT_CALLBACK\"],\"inventory-service\":[\"EXPIRE_BOOKING\"]}");

        assertThat(perms.hasPermission("payment-service", "PAYMENT_CALLBACK")).isTrue();
        assertThat(perms.hasPermission("inventory-service", "EXPIRE_BOOKING")).isTrue();
    }

    @Test
    void parse_blank_yieldsEmpty() {
        InternalServicePermissions perms = InternalServicePermissions.parse("   ");

        assertThat(perms.isEmpty()).isTrue();
        assertThat(perms.hasPermission("a", "X")).isFalse();
    }

    @Test
    void hasPermission_rejectsMissingServiceIdOrPermission() {
        InternalServicePermissions perms =
                InternalServicePermissions.parse("{\"svc\":[\"PERM_A\"]}");

        assertThat(perms.hasPermission(null, "PERM_A")).isFalse();
        assertThat(perms.hasPermission("", "PERM_A")).isFalse();
        assertThat(perms.hasPermission("svc", null)).isFalse();
        assertThat(perms.hasPermission("svc", "")).isFalse();
    }

    @Test
    void parse_invalidJson_throws() {
        assertThatThrownBy(() -> InternalServicePermissions.parse("not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("service-permissions-json");
    }
}
