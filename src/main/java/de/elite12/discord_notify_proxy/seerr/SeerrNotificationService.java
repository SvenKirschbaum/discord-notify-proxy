package de.elite12.discord_notify_proxy.seerr;

import de.elite12.discord_notify_proxy.discord.DiscordService;
import de.elite12.discord_notify_proxy.seerr.model.DeliveryStatus;
import de.elite12.discord_notify_proxy.seerr.model.MediaAvailabilityStatus;
import de.elite12.discord_notify_proxy.seerr.model.MediaType;
import de.elite12.discord_notify_proxy.seerr.model.SeerrNotificationResult;
import de.elite12.discord_notify_proxy.seerr.model.SeerrWebhookPayload;
import de.elite12.discord_notify_proxy.seerr.model.NotificationType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class SeerrNotificationService {

    private static final int DESCRIPTION_LIMIT = 900;

    private final DiscordService discordService;

    public SeerrNotificationService(DiscordService discordService) {
        this.discordService = discordService;
    }

    public SeerrNotificationResult process(SeerrWebhookPayload payload) {
        Set<Long> recipientIds = resolveRecipientIds(payload);
        if (recipientIds.isEmpty()) {
            return new SeerrNotificationResult(DeliveryStatus.IGNORED, 0, "No Discord recipients in user notification context");
        }

        MessageEmbed embed = buildEmbed(payload);
        recipientIds.forEach(discordUserId -> discordService.sendDirectMessage(discordUserId, embed));

        return new SeerrNotificationResult(DeliveryStatus.SENT, recipientIds.size(), "Queued Discord direct messages");
    }

    MessageEmbed buildEmbed(SeerrWebhookPayload payload) {
        String targetUrl = resolveTargetUrl(payload);
        String subject = shorten(payload.subject(), 220);
        String description = buildDescription(payload);
        String status = humanizeStatus(payload);
        MediaType mediaType = payload.resolvedMediaType();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setAuthor(resolveAuthor(payload))
                .setTitle(subject, targetUrl)
                .setDescription(description)
                .setColor(resolveColor(payload))
                .setTimestamp(Instant.now())
                .addField("Status", status, true)
                .addField("Media", humanizeMediaType(mediaType), true)
                .addField(resolveActorLabel(payload), resolveActorName(payload), true)
                .setFooter(payload.notificationType().name());

        if (isIssueNotification(payload.notificationType())) {
            Long issueId = payload.resolvedIssueId();
            if (issueId != null) {
                embedBuilder.addField("Issue", "#" + issueId, true);
            }
            if (payload.notificationType() == NotificationType.ISSUE_COMMENT) {
                String reporterName = payload.reporterName();
                if (!reporterName.equals(resolveActorName(payload))) {
                    embedBuilder.addField("Reporter", reporterName, true);
                }
            }
        } else {
            Long requestId = payload.resolvedRequestId();
            if (requestId != null) {
                embedBuilder.addField("Request", "#" + requestId, true);
            }
        }

        String thumbnail = normalizedUrlOrNull(payload.image());
        if (thumbnail != null) {
            embedBuilder.setThumbnail(thumbnail);
        }

        return embedBuilder.build();
    }

    private Set<Long> resolveRecipientIds(SeerrWebhookPayload payload) {
        List<String> notifyUserDiscordIds = payload.resolvedNotifyUserDiscordIds();
        List<String> preferredIds = hasValues(notifyUserDiscordIds)
                ? notifyUserDiscordIds
                : fallbackRecipientIds(payload);

        Set<Long> recipientIds = new LinkedHashSet<>();
        for (String rawId : preferredIds) {
            if (rawId == null || rawId.isBlank()) {
                continue;
            }

            try {
                recipientIds.add(Long.parseLong(rawId.trim()));
            } catch (NumberFormatException ignored) {
                // Ignore malformed IDs from upstream payloads.
            }
        }

        return recipientIds;
    }

    private String buildDescription(SeerrWebhookPayload payload) {
        StringBuilder description = new StringBuilder();
        description.append("**").append(shorten(payload.event(), 180)).append("**");

        switch (payload.notificationType()) {
            case ISSUE_COMMENT -> appendIssueCommentDescription(description, payload);
            case ISSUE_CREATED, ISSUE_RESOLVED -> appendSummary(description, payload.message());
            default -> appendSummary(description, payload.message());
        }

        return description.toString();
    }

    private void appendIssueCommentDescription(StringBuilder description, SeerrWebhookPayload payload) {
        appendSummary(description, payload.resolvedCommentMessage());

        String issueSummary = normalizedTextOrNull(payload.message());
        if (issueSummary != null) {
            description.append("\n\n**Issue**\n").append(shorten(issueSummary, DESCRIPTION_LIMIT / 2));
        }
    }

    private void appendSummary(StringBuilder description, String summary) {
        String normalizedSummary = normalizedTextOrNull(summary);
        if (normalizedSummary != null) {
            description.append("\n\n").append(shorten(normalizedSummary, DESCRIPTION_LIMIT));
        }
    }

    private Color resolveColor(SeerrWebhookPayload payload) {
        return switch (payload.notificationType()) {
            case MEDIA_APPROVED, MEDIA_AUTO_APPROVED, ISSUE_RESOLVED -> new Color(46, 204, 113);
            case MEDIA_DECLINED, MEDIA_FAILED, ISSUE_CREATED -> new Color(231, 76, 60);
            case MEDIA_AVAILABLE, ISSUE_COMMENT -> new Color(52, 152, 219);
            case MEDIA_PENDING -> new Color(241, 196, 15);
            case TEST_NOTIFICATION, UNKNOWN -> resolveColorFromMediaStatus(payload.resolvedMediaStatus());
        };
    }

    private Color resolveColorFromMediaStatus(MediaAvailabilityStatus mediaStatus) {
        return switch (mediaStatus) {
            case AVAILABLE, PARTIALLY_AVAILABLE -> new Color(52, 152, 219);
            case PENDING, PROCESSING -> new Color(241, 196, 15);
            case UNKNOWN -> new Color(149, 165, 166);
        };
    }

    private String humanizeStatus(SeerrWebhookPayload payload) {
        String notificationStatus = statusFromNotificationType(payload.notificationType());
        if (notificationStatus != null) {
            return notificationStatus;
        }

        MediaAvailabilityStatus mediaStatus = payload.resolvedMediaStatus();
        if (mediaStatus != MediaAvailabilityStatus.UNKNOWN) {
            return humanizeEnum(mediaStatus.name());
        }

        if (payload.notificationType() != NotificationType.UNKNOWN) {
            return humanizeEnum(payload.notificationType().name().replaceFirst("^MEDIA_", ""));
        }

        return payload.event().trim();
    }

    private String statusFromNotificationType(NotificationType notificationType) {
        return switch (notificationType) {
            case MEDIA_PENDING -> "Pending";
            case MEDIA_APPROVED -> "Approved";
            case MEDIA_DECLINED -> "Declined";
            case MEDIA_AVAILABLE -> "Available";
            case MEDIA_AUTO_APPROVED -> "Auto approved";
            case MEDIA_FAILED -> "Failed";
            case ISSUE_CREATED -> "Open";
            case ISSUE_RESOLVED -> "Resolved";
            case ISSUE_COMMENT -> "Commented";
            case TEST_NOTIFICATION, UNKNOWN -> null;
        };
    }

    private String resolveAuthor(SeerrWebhookPayload payload) {
        return isIssueNotification(payload.notificationType()) ? "Seerr issue update" : "Seerr request update";
    }

    private String resolveActorLabel(SeerrWebhookPayload payload) {
        return switch (payload.notificationType()) {
            case ISSUE_CREATED, ISSUE_RESOLVED -> "Reporter";
            case ISSUE_COMMENT -> "Commenter";
            default -> "Requester";
        };
    }

    private String resolveActorName(SeerrWebhookPayload payload) {
        return switch (payload.notificationType()) {
            case ISSUE_CREATED, ISSUE_RESOLVED -> payload.reporterName();
            case ISSUE_COMMENT -> payload.commenterName();
            default -> payload.requesterName();
        };
    }

    private List<String> fallbackRecipientIds(SeerrWebhookPayload payload) {
        if (isIssueNotification(payload.notificationType())) {
            if (hasValues(payload.resolvedReportedByDiscordIds())) {
                return payload.resolvedReportedByDiscordIds();
            }
            return payload.resolvedCommentedByDiscordIds();
        }
        return payload.resolvedRequestedByDiscordIds();
    }

    private boolean isIssueNotification(NotificationType notificationType) {
        return notificationType == NotificationType.ISSUE_CREATED
                || notificationType == NotificationType.ISSUE_RESOLVED
                || notificationType == NotificationType.ISSUE_COMMENT;
    }

    private String resolveTargetUrl(SeerrWebhookPayload payload) {
        String appUrl = payload.normalizedAppUrl();
        if (appUrl != null) {
            if (isIssueNotification(payload.notificationType())) {
                Long issueId = payload.resolvedIssueId();
                if (issueId != null) {
                    return appUrl + "/issues/" + issueId;
                }
            }

            Long tmdbId = payload.resolvedMediaTmdbId();
            if (tmdbId != null) {
                MediaType mediaType = payload.resolvedMediaType();
                if (mediaType == MediaType.MOVIE || mediaType == MediaType.TV) {
                    return appUrl + "/" + mediaType.name().toLowerCase(Locale.ROOT) + "/" + tmdbId;
                }
            }
        }

        return normalizedUrlOrNull(payload.requestUrl());
    }

    private String humanizeEnum(String value) {
        return value.replace('_', ' ');
    }

    private String humanizeMediaType(MediaType mediaType) {
        return switch (mediaType) {
            case MOVIE -> "Movie";
            case TV -> "TV";
            case UNKNOWN -> "Unknown";
        };
    }

    private boolean hasValues(List<String> values) {
        return values.stream().anyMatch(value -> value != null && !value.isBlank());
    }

    private String normalizedTextOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String normalizedUrlOrNull(String value) {
        String trimmed = normalizedTextOrNull(value);
        if (trimmed == null) {
            return null;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        return null;
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
