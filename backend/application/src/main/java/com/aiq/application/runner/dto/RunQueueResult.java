package com.aiq.application.runner.dto;

import java.util.UUID;

public record RunQueueResult(
    UUID queueId,
    int executedPrompts,
    boolean stoppedOnError,
    String reason
) {
}
