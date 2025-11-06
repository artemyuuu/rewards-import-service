package ru.mephi.rewards.importservice.service.model;

public record ImportError(
        int row,
        String message
) {
}
