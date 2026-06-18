package com.aiq.application.prompt.dto;

import com.aiq.domain.queue.PromptStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record PromptDetails(
    UUID id,
    UUID queueId,
    UUID targetAiToolId,
    String title,
    String content,
    PromptStatus status,
    int priority,
    long position,
    Optional<String> workingDirectoryOverride,
    int attemptCount,
    int maxAttempts,
    Instant createdAt,
    Instant updatedAt,
    Optional<Instant> startedAt,
    Optional<Instant> finishedAt,
    Optional<String> failureReason
) {
}
