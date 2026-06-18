package com.aiq.domain.queue;

public enum PromptStatus {
    DRAFT,
    QUEUED,
    WAITING_LIMIT,
    WAITING_CONFIRMATION,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED,
    SKIPPED
}
