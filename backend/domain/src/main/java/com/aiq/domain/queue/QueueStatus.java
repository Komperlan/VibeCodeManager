package com.aiq.domain.queue;

public enum QueueStatus {
    CREATED,
    WAITING_LIMIT,
    WAITING_CONFIRMATION,
    RUNNING,
    PAUSED,
    STOPPED,
    COMPLETED,
    DISABLED
}