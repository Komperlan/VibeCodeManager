package com.aiq.infrastructure.persistence.mapper;

import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.safety.WorkingHours;
import com.aiq.infrastructure.persistence.entity.PromptQueueJpaEntity;
import java.time.Duration;

public final class PromptQueuePersistenceMapper {

    private PromptQueuePersistenceMapper() {
    }

    public static PromptQueueJpaEntity toEntity(PromptQueue queue) {
        QueueExecutionPolicy policy = queue.getExecutionPolicy();
        WorkingHours workingHours = policy.workingHours();

        return new PromptQueueJpaEntity(
            queue.getId(),
            queue.getProjectId(),
            queue.getName(),
            queue.getStatus(),
            policy.autoRunMode(),
            policy.maxPromptsPerRun(),
            policy.cooldown().toMillis(),
            policy.stopOnError(),
            policy.workingHoursEnabled(),
            workingHours == null ? null : workingHours.from(),
            workingHours == null ? null : workingHours.to(),
            workingHours == null ? null : workingHours.zoneId().getId(),
            queue.getCreatedAt(),
            queue.getUpdatedAt()
        );
    }

    public static PromptQueue toDomain(PromptQueueJpaEntity entity) {
        WorkingHours workingHours = entity.isWorkingHoursEnabled()
            ? new WorkingHours(
                entity.getWorkingHoursFrom(),
                entity.getWorkingHoursTo(),
                java.time.ZoneId.of(entity.getWorkingHoursZone())
            )
            : null;

        QueueExecutionPolicy policy = new QueueExecutionPolicy(
            entity.getAutoRunMode(),
            entity.getMaxPromptsPerRun(),
            Duration.ofMillis(entity.getCooldownMillis()),
            entity.isStopOnError(),
            entity.isWorkingHoursEnabled(),
            workingHours
        );

        return PromptQueue.restore(
            entity.getId(),
            entity.getProjectId(),
            entity.getName(),
            entity.getStatus(),
            policy,
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
