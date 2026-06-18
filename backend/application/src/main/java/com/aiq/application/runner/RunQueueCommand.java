package com.aiq.application.runner;

import java.util.UUID;

public record RunQueueCommand(
    UUID queueId,
    int maxPrompts
) {
}
