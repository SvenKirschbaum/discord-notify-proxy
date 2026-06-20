package de.elite12.discord_notify_proxy.observability;

import de.elite12.discord_notify_proxy.seerr.model.DeliveryStatus;
import de.elite12.discord_notify_proxy.seerr.model.MediaType;
import de.elite12.discord_notify_proxy.seerr.model.NotificationType;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final MeterRegistry meterRegistry;

    public NotificationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordWebhookReceived(NotificationType notificationType, MediaType mediaType) {
        meterRegistry.counter("seerr.webhook.received", seerrTags(notificationType, mediaType)).increment();
    }

    public void recordWebhookIgnored(NotificationType notificationType, MediaType mediaType, String recipientSource) {
        meterRegistry.counter("seerr.webhook.ignored",
                seerrTags(notificationType, mediaType).and("recipient.source", recipientSource)).increment();
    }

    public void recordMalformedRecipientId(NotificationType notificationType, String recipientSource) {
        meterRegistry.counter("seerr.recipient.malformed",
                Tags.of("notification.type", notificationType.name(), "recipient.source", recipientSource)).increment();
    }

    public void recordRecipientCount(NotificationType notificationType, MediaType mediaType, String recipientSource, int recipientCount) {
        DistributionSummary.builder("seerr.recipient.count")
                .tags(seerrTags(notificationType, mediaType).and("recipient.source", recipientSource))
                .register(meterRegistry)
                .record(recipientCount);
    }

    public void recordWebhookProcessed(NotificationType notificationType, MediaType mediaType, DeliveryStatus deliveryStatus) {
        meterRegistry.counter("seerr.webhook.processed",
                seerrTags(notificationType, mediaType).and("delivery.status", deliveryStatus.name())).increment();
    }

    public void recordDiscordDeliveryEnqueued() {
        meterRegistry.counter("discord.dm.enqueued").increment();
    }

    public void recordDiscordDeliverySent() {
        meterRegistry.counter("discord.dm.sent").increment();
    }

    public void recordDiscordDeliveryFailed(String stage) {
        meterRegistry.counter("discord.dm.failed", "stage", stage).increment();
    }

    private Tags seerrTags(NotificationType notificationType, MediaType mediaType) {
        return Tags.of("notification.type", notificationType.name(), "media.type", mediaType.name());
    }
}
