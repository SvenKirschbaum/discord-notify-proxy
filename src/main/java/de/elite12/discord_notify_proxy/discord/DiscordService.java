package de.elite12.discord_notify_proxy.discord;

import de.elite12.discord_notify_proxy.observability.NotificationMetrics;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiscordService {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

    private final JDA jda;
    private final Tracer tracer;
    private final NotificationMetrics notificationMetrics;

    public DiscordService(JDA jda, OpenTelemetry openTelemetry, NotificationMetrics notificationMetrics) {
        this.jda = jda;
        this.tracer = openTelemetry.getTracer(DiscordService.class.getName());
        this.notificationMetrics = notificationMetrics;
    }

    @WithSpan("discord.dm.enqueue")
    public void sendDirectMessage(long discordUserId, MessageEmbed embed) {
        Context parentContext = Context.current();
        Span deliverySpan = tracer.spanBuilder("discord.dm.deliver")
                .setParent(parentContext)
                .startSpan();
        Context deliveryContext = parentContext.with(deliverySpan);

        notificationMetrics.recordDiscordDeliveryEnqueued();

        try (Scope ignored = deliveryContext.makeCurrent()) {
            deliverySpan.setAttribute("discord.user_id", String.valueOf(discordUserId));
            deliverySpan.addEvent("discord.delivery.started");
        }

        this.jda.retrieveUserById(discordUserId).queue(user -> withDeliveryScope(deliveryContext, deliverySpan, () -> {
                    deliverySpan.addEvent("discord.user.resolved");
                    user.openPrivateChannel().queue(channel -> withDeliveryScope(deliveryContext, deliverySpan, () -> {
                                        deliverySpan.addEvent("discord.dm.channel.opened");
                                        channel.sendMessageEmbeds(embed).queue(
                                                message -> withDeliveryScope(deliveryContext, deliverySpan, () -> {
                                                    deliverySpan.addEvent("discord.dm.sent");
                                                    notificationMetrics.recordDiscordDeliverySent();
                                                    log.info("Sent Seerr notification to Discord user {}", discordUserId);
                                                    deliverySpan.end();
                                                }),
                                                error -> failDelivery(deliveryContext, deliverySpan, discordUserId, "send_message", "Failed to send Seerr notification to Discord user {}", error)
                                        );
                                    }),
                            error -> failDelivery(deliveryContext, deliverySpan, discordUserId, "open_channel", "Failed to open DM channel for Discord user {}", error)
                    );
                }),
                error -> failDelivery(deliveryContext, deliverySpan, discordUserId, "resolve_user", "Failed to resolve Discord user {}", error)
        );
    }

    private void failDelivery(Context deliveryContext, Span deliverySpan, long discordUserId, String stage, String message, Throwable error) {
        withDeliveryScope(deliveryContext, deliverySpan, () -> {
            deliverySpan.addEvent("discord.dm.failed");
            deliverySpan.setAttribute("discord.failure_stage", stage);
            deliverySpan.recordException(error);
            deliverySpan.setStatus(StatusCode.ERROR, stage);
            notificationMetrics.recordDiscordDeliveryFailed(stage);
            log.warn(message, discordUserId, error);
            deliverySpan.end();
        });
    }

    private void withDeliveryScope(Context deliveryContext, Span deliverySpan, Runnable action) {
        try (Scope ignored = deliveryContext.makeCurrent()) {
            action.run();
        } catch (RuntimeException error) {
            deliverySpan.recordException(error);
            deliverySpan.setStatus(StatusCode.ERROR, error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
            deliverySpan.end();
            throw error;
        }
    }
}
