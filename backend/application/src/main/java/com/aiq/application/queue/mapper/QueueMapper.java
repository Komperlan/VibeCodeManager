package com.aiq.application.queue.mapper;

import com.aiq.application.queue.dto.CreateQueueResult;
import com.aiq.application.queue.dto.QueueDetails;
import com.aiq.application.queue.dto.QueueSummary;
import com.aiq.domain.queue.PromptQueue;

public final class QueueMapper {

    private QueueMapper() {
    }

    public static CreateQueueResult toCreateQueueResult(PromptQueue queue) {
        return new CreateQueueResult(queue.getId());
    }

    public static QueueDetails toDetails(PromptQueue queue) {
        return new QueueDetails(
            queue.getId(),
            queue.getProjectId(),
            queue.getName(),
            queue.getStatus(),
            queue.getExecutionPolicy(),
            queue.getCreatedAt(),
            queue.getUpdatedAt()
        );
    }

    public static QueueSummary toSummary(PromptQueue queue) {
        return new QueueSummary(
            queue.getId(),
            queue.getName(),
            queue.getStatus()
        );
    }
}
