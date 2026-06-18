package com.aiq.application.queue;

import com.aiq.domain.queue.QueueExecutionPolicy;

public record ChangeQueuePolicyCommand(
    QueueExecutionPolicy executionPolicy
) {
}
