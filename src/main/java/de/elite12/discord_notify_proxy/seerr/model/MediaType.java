package de.elite12.discord_notify_proxy.seerr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum MediaType {
    MOVIE("movie"),
    TV("tv"),
    @JsonEnumDefaultValue
    UNKNOWN("unknown");

    private final String wireValue;

    MediaType(String wireValue) {
        this.wireValue = wireValue;
    }

    @JsonValue
    public String getWireValue() {
        return wireValue;
    }

    @JsonCreator
    public static MediaType fromWireValue(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        return Arrays.stream(values())
                .filter(mediaType -> mediaType.wireValue.equalsIgnoreCase(value) || mediaType.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(UNKNOWN);
    }
}
