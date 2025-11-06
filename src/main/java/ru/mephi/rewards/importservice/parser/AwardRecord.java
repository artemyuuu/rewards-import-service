package ru.mephi.rewards.importservice.parser;

import java.time.OffsetDateTime;

public record AwardRecord(
        Long employeeId,
        String employeeFullName,
        Long rewardId,
        String rewardName,
        OffsetDateTime awardedAt,
        int rowNumber
) {
}
