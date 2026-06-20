package de.elite12.discord_notify_proxy.seerr.model;

public record SeerrNotificationResult(
        DeliveryStatus status,
        int recipientCount,
        String detail
) {
}
