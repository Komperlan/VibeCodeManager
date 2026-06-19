package com.aiq.application.port.out;

import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromptQueueRepository {

    PromptQueue save(PromptQueue queue);

    Optional<PromptQueue> findById(UUID queueId);

    Optional<PromptQueue> findByIdForUpdate(UUID queueId);

    List<PromptQueue> findByProjectId(UUID projectId);

    List<PromptQueue> findByStatuses(Collection<QueueStatus> statuses);
}
