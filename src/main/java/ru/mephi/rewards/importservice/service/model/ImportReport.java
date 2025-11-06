package ru.mephi.rewards.importservice.service.model;

import java.util.List;

public record ImportReport(
        int processed,
        int saved,
        int skipped,
        List<ImportError> errors
) {
}
