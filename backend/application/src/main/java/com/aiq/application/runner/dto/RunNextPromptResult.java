package com.aiq.application.runner.dto;

import java.util.UUID;

public record RunNextPromptResult(
    UUID queueId,
    UUID promptId,
    boolean executed,
    String reason
) {
}
