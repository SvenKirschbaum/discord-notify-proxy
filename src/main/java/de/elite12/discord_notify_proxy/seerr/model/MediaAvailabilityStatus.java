package de.elite12.discord_notify_proxy.seerr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MediaAvailabilityStatus {
    @JsonEnumDefaultValue
    UNKNOWN("UNKNOWN"),
    PENDING("PENDING"),
    PROCESSING("PROCESSING"),
    PARTIALLY_AVAILABLE("PARTIALLY_AVAILABLE"),
    AVAILABLE("AVAILABLE");

    private final String wireValue;

    MediaAvailabilityStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static MediaAvailabilityStatus fromWireValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
                .filter(status -> status.wireValue.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
