package com.aiq.application.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.runner.dto.RunQueueResult;
import com.aiq.application.service.QueueRunnerApplicationService;
import com.aiq.domain.queue.AutoRunMode;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.queue.QueueStatus;
import com.aiq.domain.safety.WorkingHours;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WaitingLimitQueueSchedulerTest {

    private final PromptQueueRepository promptQueueRepository = mock(PromptQueueRepository.class);
    private final QueueRunnerApplicationService queueRunnerApplicationService = mock(QueueRunnerApplicationService.class);
    private final WaitingLimitQueueScheduler scheduler = new WaitingLimitQueueScheduler(
        promptQueueRepository,
        queueRunnerApplicationService
    );

    @Test
    void shouldResumeEveryQueueWaitingForLimitAndIsolateFailures() {
        PromptQueue first = waitingQueue(policy(2, false, null));
        PromptQueue second = waitingQueue(policy(3, false, null));
        when(promptQueueRepository.findByStatuses(Set.of(QueueStatus.WAITING_LIMIT)))
            .thenReturn(List.of(first, second));
        when(queueRunnerApplicationService.resumeWaitingLimitQueue(first.getId()))
            .thenThrow(new IllegalStateException("First queue failed"));
        when(queueRunnerApplicationService.resumeWaitingLimitQueue(second.getId()))
            .thenReturn(new RunQueueResult(second.getId(), 1, false, "Prompt executed successfully"));

        scheduler.resumeWaitingQueues();

        verify(queueRunnerApplicationService).resumeWaitingLimitQueue(first.getId());
        verify(queueRunnerApplicationService).resumeWaitingLimitQueue(second.getId());
    }

    @Test
    void shouldSkipQueueOutsideConfiguredWorkingHours() {
        ZoneId zoneId = ZoneId.systemDefault();
        LocalTime now = Instant.now().atZone(zoneId).toLocalTime();
        WorkingHours futureWindow = new WorkingHours(
            now.plusMinutes(5),
            now.plusMinutes(10),
            zoneId
        );
        PromptQueue queue = waitingQueue(policy(1, true, futureWindow));
        when(promptQueueRepository.findByStatuses(Set.of(QueueStatus.WAITING_LIMIT)))
            .thenReturn(List.of(queue));

        scheduler.resumeWaitingQueues();

        verify(queueRunnerApplicationService, never()).resumeWaitingLimitQueue(queue.getId());
    }

    private PromptQueue waitingQueue(QueueExecutionPolicy policy) {
        PromptQueue queue = PromptQueue.create(UUID.randomUUID(), "Waiting queue", policy);
        queue.markWaitingLimit();
        return queue;
    }

    private QueueExecutionPolicy policy(
        int maxPrompts,
        boolean workingHoursEnabled,
        WorkingHours workingHours
    ) {
        return new QueueExecutionPolicy(
            AutoRunMode.ASK_CONFIRMATION,
            maxPrompts,
            Duration.ZERO,
            true,
            workingHoursEnabled,
            workingHours
        );
    }
}
