package com.aiq.application.queue.dto;

import com.aiq.domain.queue.QueueStatus;
import java.util.UUID;

public record QueueSummary(
    UUID id,
    String name,
    QueueStatus status
) {
}
