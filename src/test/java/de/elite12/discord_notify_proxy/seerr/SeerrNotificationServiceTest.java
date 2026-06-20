package de.elite12.discord_notify_proxy.seerr;

import de.elite12.discord_notify_proxy.discord.DiscordService;
import de.elite12.discord_notify_proxy.observability.NotificationMetrics;
import de.elite12.discord_notify_proxy.seerr.model.DeliveryStatus;
import de.elite12.discord_notify_proxy.seerr.model.MediaAvailabilityStatus;
import de.elite12.discord_notify_proxy.seerr.model.MediaType;
import de.elite12.discord_notify_proxy.seerr.model.NotificationType;
import de.elite12.discord_notify_proxy.seerr.model.SeerrNotificationResult;
import de.elite12.discord_notify_proxy.seerr.model.SeerrWebhookPayload;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.aot.DisabledInAotMode;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisabledInAotMode("Uses Mockito mocks that are only needed for JVM tests")
class SeerrNotificationServiceTest {

    private final DiscordService discordService = mock(DiscordService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final NotificationMetrics notificationMetrics = new NotificationMetrics(meterRegistry);
    private final SeerrNotificationService service = new SeerrNotificationService(discordService, notificationMetrics);

    @Test
    void sendsToAllUniqueNotifyUserDiscordIds() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.MEDIA_APPROVED,
                "Your request was approved",
                "Dune: Part Two",
                "Paul Atreides unites with the Fremen while on a warpath of revenge.",
                "https://image.tmdb.org/t/p/w500/test.jpg",
                MediaType.MOVIE,
                MediaAvailabilityStatus.PENDING,
                42L,
                "https://seerr.example/request/42",
                "sven",
                List.of("108253150351200256", "108253150351200256", "999999999999999999", "bad-id"),
                "requester",
                List.of("777"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
        );

        SeerrNotificationResult result = service.process(payload);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.recipientCount()).isEqualTo(2);
        assertThat(meterRegistry.get("seerr.webhook.received").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("seerr.recipient.malformed").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("seerr.webhook.processed").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("seerr.recipient.count").summary().count()).isEqualTo(1L);
        verify(discordService, times(2)).sendDirectMessage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(MessageEmbed.class));
    }

    @Test
    void fallsBackToRequestedByDiscordIdsWhenNotifyUserIdsAreMissing() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.MEDIA_AVAILABLE,
                "Your request is now available",
                "The Expanse",
                "A thriller set two hundred years in the future.",
                "https://image.tmdb.org/t/p/w500/expanse.jpg",
                MediaType.TV,
                MediaAvailabilityStatus.AVAILABLE,
                7L,
                "https://seerr.example/request/7",
                null,
                List.of(),
                "naomi",
                List.of("108253150351200256"),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
        );

        SeerrNotificationResult result = service.process(payload);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.recipientCount()).isEqualTo(1);
        verify(discordService).sendDirectMessage(org.mockito.ArgumentMatchers.eq(108253150351200256L), org.mockito.ArgumentMatchers.any(MessageEmbed.class));
    }

    @Test
    void buildsAnEmbedWithBacklinkThumbnailAndColor() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.MEDIA_DECLINED,
                "Your request was declined",
                "Arcane",
                "A conflict erupts between Piltover and Zaun.",
                "https://image.tmdb.org/t/p/w500/arcane.jpg",
                MediaType.TV,
                MediaAvailabilityStatus.UNKNOWN,
                99L,
                "https://seerr.example/request/99",
                "vi",
                List.of("108253150351200256"),
                null,
                List.of(),
                Map.of("mediaType", "tv", "tmdbId", 94605, "status", "PENDING"),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
        );

        MessageEmbed embed = service.buildEmbed(payload);

        assertThat(embed.getTitle()).isEqualTo("Arcane");
        assertThat(embed.getUrl()).isEqualTo("https://seerr.example/tv/94605");
        assertThat(embed.getThumbnail()).isNotNull();
        assertThat(embed.getThumbnail().getUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/arcane.jpg");
        assertThat(embed.getColor()).isEqualTo(new Color(231, 76, 60));
        assertThat(embed.getDescription()).contains("Your request was declined");
        assertThat(embed.getFields()).anySatisfy(field -> {
            assertThat(field.getName()).isEqualTo("Status");
            assertThat(field.getValue()).isEqualTo("Declined");
        });
        assertThat(embed.getFields()).extracting(MessageEmbed.Field::getName)
                .contains("Status", "Media", "Requester", "Request");
    }

    @Test
    void readsRecipientsAndMetadataFromNestedSeerrObjects() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.MEDIA_AVAILABLE,
                "Your request is now available",
                "Silo",
                "A hidden bunker holds the last of humanity.",
                "https://image.tmdb.org/t/p/w500/silo.jpg",
                MediaType.UNKNOWN,
                MediaAvailabilityStatus.UNKNOWN,
                null,
                "https://seerr.example/request/314",
                null,
                List.of(),
                null,
                List.of(),
                Map.of("mediaType", "tv", "status", "AVAILABLE"),
                Map.of(
                        "requestId", 314,
                        "requestedByUsername", "juliette",
                        "requestedByDiscordIds", List.of("108253150351200256")
                ),
                Map.of(),
                Map.of(),
                List.of()
        );

        SeerrNotificationResult result = service.process(payload);
        MessageEmbed embed = service.buildEmbed(payload);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.recipientCount()).isEqualTo(1);
        assertThat(embed.getColor()).isEqualTo(new Color(52, 152, 219));
        assertThat(embed.getFields()).extracting(MessageEmbed.Field::getValue)
                .contains("TV", "juliette", "#314");
        assertThat(embed.getUrl()).isEqualTo("https://seerr.example/request/314");
    }

    @Test
    void readsStringEncodedRecipientsFromNestedSeerrObjects() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.MEDIA_AVAILABLE,
                "Your request is now available",
                "Andor",
                "Cassian is drawn into the Rebellion.",
                null,
                MediaType.UNKNOWN,
                MediaAvailabilityStatus.UNKNOWN,
                null,
                "https://seerr.example/request/77",
                null,
                List.of(),
                null,
                List.of(),
                Map.of("mediaType", "tv", "status", "AVAILABLE"),
                Map.of(
                        "requestId", "77",
                        "requestedByUsername", "cassian",
                        "requestedByDiscordIds", "[\"108253150351200256\", \"999999999999999999\"]"
                ),
                Map.of(),
                Map.of(),
                List.of()
        );

        SeerrNotificationResult result = service.process(payload);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.recipientCount()).isEqualTo(2);
        verify(discordService, times(2)).sendDirectMessage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(MessageEmbed.class));
    }

    @Test
    void ignoresPayloadWhenNoRecipientIdsExist() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.MEDIA_AVAILABLE,
                "Your request is now available",
                "Dune: Part Two",
                "Now streaming.",
                null,
                MediaType.MOVIE,
                MediaAvailabilityStatus.AVAILABLE,
                42L,
                "https://seerr.example/request/42",
                "sven",
                List.of(),
                "sven",
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                List.of()
        );

        SeerrNotificationResult result = service.process(payload);

        assertThat(result.status()).isEqualTo(DeliveryStatus.IGNORED);
        assertThat(result.recipientCount()).isZero();
        assertThat(meterRegistry.get("seerr.webhook.ignored").counter().count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("seerr.webhook.processed").counter().count()).isEqualTo(1.0);
        verify(discordService, never()).sendDirectMessage(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any(MessageEmbed.class));
    }

    @Test
    void buildsUsefulIssueCommentEmbedAndFallsBackToReporterRecipients() {
        SeerrWebhookPayload payload = new SeerrWebhookPayload(
                "https://seerr.example",
                NotificationType.ISSUE_COMMENT,
                "A new comment was added to your issue",
                "Silo",
                "Subtitles are delayed by a few seconds.",
                "https://image.tmdb.org/t/p/w500/silo.jpg",
                MediaType.TV,
                MediaAvailabilityStatus.UNKNOWN,
                null,
                null,
                null,
                List.of(),
                null,
                List.of(),
                Map.of("mediaType", "tv", "tmdbId", 125988),
                Map.of(),
                Map.of(
                        "issueId", 18,
                        "reportedByUsername", "juliette",
                        "reportedByDiscordIds", List.of("108253150351200256")
                ),
                Map.of(
                        "message", "I can confirm it happens in episode 3 as well.",
                        "commentedByUsername", "support-bot"
                ),
                List.of()
        );

        SeerrNotificationResult result = service.process(payload);
        MessageEmbed embed = service.buildEmbed(payload);

        assertThat(result.status()).isEqualTo(DeliveryStatus.SENT);
        assertThat(result.recipientCount()).isEqualTo(1);
        assertThat(embed.getAuthor().getName()).isEqualTo("Seerr issue update");
        assertThat(embed.getUrl()).isEqualTo("https://seerr.example/issues/18");
        assertThat(embed.getDescription())
                .contains("I can confirm it happens in episode 3 as well.")
                .contains("Subtitles are delayed by a few seconds.");
        assertThat(embed.getFields()).extracting(MessageEmbed.Field::getName)
                .contains("Status", "Media", "Commenter", "Issue", "Reporter");
        assertThat(embed.getFields()).anySatisfy(field -> {
            assertThat(field.getName()).isEqualTo("Status");
            assertThat(field.getValue()).isEqualTo("Commented");
        });
    }
}
