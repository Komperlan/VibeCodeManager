package com.aiq.application.prompt.dto;

import com.aiq.domain.queue.PromptStatus;
import java.util.UUID;

public record PromptSummary(
    UUID id,
    String title,
    PromptStatus status,
    int priority,
    long position
) {
}
