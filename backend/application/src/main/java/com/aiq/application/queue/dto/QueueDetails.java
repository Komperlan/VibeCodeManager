package com.aiq.application.queue.dto;

import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.queue.QueueStatus;
import java.time.Instant;
import java.util.UUID;

public record QueueDetails(
    UUID id,
    UUID projectId,
    String name,
    QueueStatus status,
    QueueExecutionPolicy executionPolicy,
    Instant createdAt,
    Instant updatedAt
) {
}
