package de.elite12.discord_notify_proxy.seerr.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum NotificationType {
    MEDIA_PENDING,
    MEDIA_APPROVED,
    MEDIA_DECLINED,
    MEDIA_AVAILABLE,
    MEDIA_AUTO_APPROVED,
    MEDIA_FAILED,
    TEST_NOTIFICATION,
    ISSUE_CREATED,
    ISSUE_RESOLVED,
    ISSUE_COMMENT,
    @JsonEnumDefaultValue
    UNKNOWN
}
