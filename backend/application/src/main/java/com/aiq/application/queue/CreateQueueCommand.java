package com.aiq.application.queue;

import com.aiq.domain.queue.QueueExecutionPolicy;
import java.util.UUID;

public record CreateQueueCommand(
    UUID projectId,
    String name,
    QueueExecutionPolicy executionPolicy
) {
}
