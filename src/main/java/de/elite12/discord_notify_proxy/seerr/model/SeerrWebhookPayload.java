package de.elite12.discord_notify_proxy.seerr.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeerrWebhookPayload(
        String appUrl,
        @NotNull NotificationType notificationType,
        @NotBlank String event,
        @NotBlank String subject,
        String message,
        String image,
        MediaType mediaType,
        MediaAvailabilityStatus mediaStatus,
        Long requestId,
        String requestUrl,
        String notifyUsername,
        Object notifyUserDiscordIds,
        String requestedByUsername,
        Object requestedByDiscordIds,
        Map<String, ?> media,
        Map<String, ?> request,
        Map<String, ?> issue,
        Map<String, ?> comment,
        List<Map<String, ?>> extra
) {

    public SeerrWebhookPayload {
        mediaType = mediaType == null ? MediaType.UNKNOWN : mediaType;
        mediaStatus = mediaStatus == null ? MediaAvailabilityStatus.UNKNOWN : mediaStatus;
        extra = extra == null ? List.of() : List.copyOf(extra);
    }

    public String requesterName() {
        if (notifyUsername != null && !notifyUsername.isBlank()) {
            return notifyUsername.trim();
        }
        if (requestedByUsername != null && !requestedByUsername.isBlank()) {
            return requestedByUsername.trim();
        }
        String nestedRequestedByUsername = nestedRequestString("requestedByUsername");
        if (nestedRequestedByUsername != null && !nestedRequestedByUsername.isBlank()) {
            return nestedRequestedByUsername.trim();
        }
        return "Unknown user";
    }

    public List<String> resolvedNotifyUserDiscordIds() {
        return stringifyDiscordIds(notifyUserDiscordIds);
    }

    public List<String> resolvedRequestedByDiscordIds() {
        List<String> topLevelIds = stringifyDiscordIds(requestedByDiscordIds);
        if (!topLevelIds.isEmpty()) {
            return topLevelIds;
        }

        Object rawValue = request == null ? null : request.get("requestedByDiscordIds");
        if (rawValue instanceof List<?> rawList) {
            return stringifyList(rawList);
        }
        if (rawValue instanceof String value) {
            return parseDiscordIds(value);
        }

        return List.of();
    }

    public Long resolvedRequestId() {
        if (requestId != null) {
            return requestId;
        }

        Object rawValue = request == null ? null : request.get("requestId");
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        if (rawValue instanceof String value && !value.isBlank()) {
            String digits = value.trim();
            if (digits.startsWith("\"") && digits.endsWith("\"") && digits.length() > 1) {
                digits = digits.substring(1, digits.length() - 1);
            }
            return Long.parseLong(digits);
        }

        return null;
    }

    public Long resolvedIssueId() {
        return nestedLong(issue, "issueId");
    }

    public MediaType resolvedMediaType() {
        if (mediaType != MediaType.UNKNOWN) {
            return mediaType;
        }

        return MediaType.fromWireValue(nestedMediaString("mediaType"));
    }

    public MediaAvailabilityStatus resolvedMediaStatus() {
        if (mediaStatus != MediaAvailabilityStatus.UNKNOWN) {
            return mediaStatus;
        }

        return MediaAvailabilityStatus.fromWireValue(nestedMediaString("status"));
    }

    public Long resolvedMediaTmdbId() {
        return nestedLong(media, "tmdbId");
    }

    public String reporterName() {
        String value = nestedIssueString("reportedByUsername");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return "Unknown user";
    }

    public List<String> resolvedReportedByDiscordIds() {
        return nestedDiscordIds(issue, "reportedByDiscordIds");
    }

    public String commenterName() {
        String value = nestedCommentString("commentedByUsername");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return reporterName();
    }

    public List<String> resolvedCommentedByDiscordIds() {
        return nestedDiscordIds(comment, "commentedByDiscordIds");
    }

    public String resolvedCommentMessage() {
        String value = nestedCommentString("message");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return null;
    }

    public String normalizedAppUrl() {
        if (appUrl == null || appUrl.isBlank()) {
            return null;
        }

        String trimmed = appUrl.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            return null;
        }

        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String nestedRequestString(String key) {
        Object value = request == null ? null : request.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String nestedIssueString(String key) {
        Object value = issue == null ? null : issue.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String nestedCommentString(String key) {
        Object value = comment == null ? null : comment.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String nestedMediaString(String key) {
        Object value = media == null ? null : media.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Long nestedLong(Map<String, ?> source, String key) {
        Object rawValue = source == null ? null : source.get(key);
        if (rawValue instanceof Number number) {
            return number.longValue();
        }
        if (rawValue instanceof String value && !value.isBlank()) {
            String digits = value.trim();
            if (digits.startsWith("\"") && digits.endsWith("\"") && digits.length() > 1) {
                digits = digits.substring(1, digits.length() - 1);
            }
            return Long.parseLong(digits);
        }
        return null;
    }

    private List<String> nestedDiscordIds(Map<String, ?> source, String key) {
        Object rawValue = source == null ? null : source.get(key);
        if (rawValue instanceof List<?> rawList) {
            return stringifyList(rawList);
        }
        if (rawValue instanceof String value) {
            return parseDiscordIds(value);
        }
        return List.of();
    }

    private List<String> stringifyList(List<?> rawList) {
        List<String> values = new ArrayList<>();
        for (Object element : rawList) {
            if (element != null) {
                values.add(String.valueOf(element));
            }
        }
        return List.copyOf(values);
    }

    private List<String> stringifyDiscordIds(Object rawValue) {
        if (rawValue instanceof List<?> rawList) {
            return stringifyList(rawList);
        }
        if (rawValue instanceof String value) {
            return parseDiscordIds(value);
        }
        return List.of();
    }

    private List<String> parseDiscordIds(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }

        String normalized = trimmed;
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }

        if (normalized.isBlank()) {
            return List.of();
        }

        String[] tokens = normalized.split(",");
        List<String> values = new ArrayList<>();
        for (String token : tokens) {
            String candidate = token.trim();
            if (candidate.startsWith("\"") && candidate.endsWith("\"") && candidate.length() > 1) {
                candidate = candidate.substring(1, candidate.length() - 1);
            }
            if (!candidate.isBlank()) {
                values.add(candidate);
            }
        }

        return List.copyOf(values);
    }
}
