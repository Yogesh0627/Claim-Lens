package com.niyotechnologies.claimlens.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        String title,
        String message,
        Boolean read,
        Instant createdAt
) {
}
