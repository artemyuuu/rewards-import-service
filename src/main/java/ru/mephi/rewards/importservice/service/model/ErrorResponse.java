package ru.mephi.rewards.importservice.service.model;

import java.time.OffsetDateTime;
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message
) {
}

