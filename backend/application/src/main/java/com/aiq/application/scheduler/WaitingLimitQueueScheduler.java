package com.aiq.application.scheduler;

import com.aiq.application.port.out.PromptQueueRepository;
import com.aiq.application.runner.dto.RunQueueResult;
import com.aiq.application.service.QueueRunnerApplicationService;
import com.aiq.domain.queue.PromptQueue;
import com.aiq.domain.queue.QueueExecutionPolicy;
import com.aiq.domain.queue.QueueStatus;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"aiq.executor.codex.enabled", "aiq.queue.limit-resume.enabled"},
    havingValue = "true"
)
@Slf4j
public class WaitingLimitQueueScheduler {

    private final PromptQueueRepository promptQueueRepository;
    private final QueueRunnerApplicationService queueRunnerApplicationService;

    @Scheduled(
        fixedDelayString = "${aiq.queue.limit-resume.poll-interval:60s}",
        initialDelayString = "${aiq.queue.limit-resume.poll-interval:60s}"
    )
    public void resumeWaitingQueues() {
        var queues = promptQueueRepository.findByStatuses(Set.of(QueueStatus.WAITING_LIMIT));
        if (queues.isEmpty()) {
            return;
        }

        log.info("Checking {} queues waiting for AI limits", queues.size());
        Instant now = Instant.now();
        for (PromptQueue queue : queues) {
            if (!isWithinWorkingHours(queue, now)) {
                log.debug("Skipping queue {} outside configured working hours", queue.getId());
                continue;
            }

            resumeQueue(queue);
        }
    }

    private void resumeQueue(PromptQueue queue) {
        try {
            RunQueueResult result = queueRunnerApplicationService.resumeWaitingLimitQueue(queue.getId());
            log.info(
                "Finished automatic limit retry for queue {}: executedPrompts={}, reason={}",
                queue.getId(),
                result.executedPrompts(),
                result.reason()
            );
        } catch (RuntimeException exception) {
            log.warn(
                "Automatic limit retry failed for queue {}: {}",
                queue.getId(),
                exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
            );
        }
    }

    private boolean isWithinWorkingHours(PromptQueue queue, Instant now) {
        QueueExecutionPolicy policy = queue.getExecutionPolicy();
        return !policy.workingHoursEnabled() || policy.workingHours().contains(now);
    }
}
