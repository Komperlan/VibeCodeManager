package com.aiq.infrastructure.persistence.mapper;

import com.aiq.domain.queue.Prompt;
import com.aiq.infrastructure.persistence.entity.PromptJpaEntity;

public final class PromptPersistenceMapper {

    private PromptPersistenceMapper() {
    }

    public static PromptJpaEntity toEntity(Prompt prompt) {
        return new PromptJpaEntity(
            prompt.getId(),
            prompt.getQueueId(),
            prompt.getTargetAiToolId(),
            prompt.getTitle(),
            prompt.getContent(),
            prompt.getStatus(),
            prompt.getPriority(),
            prompt.getPosition(),
            prompt.workingDirectoryOverride().orElse(null),
            prompt.getAttemptCount(),
            prompt.getMaxAttempts(),
            prompt.getCreatedAt(),
            prompt.getUpdatedAt(),
            prompt.startedAt().orElse(null),
            prompt.finishedAt().orElse(null),
            prompt.failureReason().orElse(null)
        );
    }

    public static Prompt toDomain(PromptJpaEntity entity) {
        return Prompt.restore(
            entity.getId(),
            entity.getQueueId(),
            entity.getTargetAiToolId(),
            entity.getTitle(),
            entity.getContent(),
            entity.getStatus(),
            entity.getPriority(),
            entity.getPosition(),
            entity.getWorkingDirectoryOverride(),
            entity.getAttemptCount(),
            entity.getMaxAttempts(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getFailureReason()
        );
    }
}
