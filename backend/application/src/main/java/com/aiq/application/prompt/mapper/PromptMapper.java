package com.aiq.application.prompt.mapper;

import com.aiq.application.prompt.dto.AddPromptResult;
import com.aiq.application.prompt.dto.PromptDetails;
import com.aiq.application.prompt.dto.PromptSummary;
import com.aiq.domain.queue.Prompt;

public final class PromptMapper {

    private PromptMapper() {
    }

    public static AddPromptResult toAddPromptResult(Prompt prompt) {
        return new AddPromptResult(prompt.getId());
    }

    public static PromptDetails toDetails(Prompt prompt) {
        return new PromptDetails(
            prompt.getId(),
            prompt.getQueueId(),
            prompt.getTargetAiToolId(),
            prompt.getTitle(),
            prompt.getContent(),
            prompt.getStatus(),
            prompt.getPriority(),
            prompt.getPosition(),
            prompt.workingDirectoryOverride(),
            prompt.getAttemptCount(),
            prompt.getMaxAttempts(),
            prompt.getCreatedAt(),
            prompt.getUpdatedAt(),
            prompt.startedAt(),
            prompt.finishedAt(),
            prompt.failureReason()
        );
    }

    public static PromptSummary toSummary(Prompt prompt) {
        return new PromptSummary(
            prompt.getId(),
            prompt.getTitle(),
            prompt.getStatus(),
            prompt.getPriority(),
            prompt.getPosition()
        );
    }
}
