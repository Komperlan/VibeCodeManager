package com.aiq.domain.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptQueueTest {

    @Test
    void shouldCreateQueueWithDefaultPolicyWhenPolicyIsNull() {
        PromptQueue promptQueue = PromptQueue.create(
            UUID.randomUUID(),
            "Backend queue",
            null
        );

        assertThat(promptQueue.getId()).isNotNull();
        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.CREATED);
        assertThat(promptQueue.getExecutionPolicy()).isEqualTo(QueueExecutionPolicy.defaultPolicy());
        assertThat(promptQueue.shouldStopOnError()).isTrue();
    }

    @Test
    void shouldStartCreatedQueue() {
        PromptQueue promptQueue = queue();

        promptQueue.start();

        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.RUNNING);
        assertThat(promptQueue.canRun()).isFalse();
    }

    @Test
    void shouldReopenCompletedQueueForNewPrompts() {
        PromptQueue promptQueue = queue();
        promptQueue.complete();

        promptQueue.reopenForNewPrompts();

        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.CREATED);
        assertThat(promptQueue.canRun()).isTrue();
    }

    @Test
    void shouldRejectReopeningQueueThatIsNotCompleted() {
        PromptQueue promptQueue = queue();

        assertThatIllegalStateException()
            .isThrownBy(promptQueue::reopenForNewPrompts)
            .withMessage("Only completed queue can be reopened for new prompts");
    }

    @Test
    void shouldPauseAndResumeRunningQueue() {
        PromptQueue promptQueue = queue();

        promptQueue.start();
        promptQueue.pause();
        promptQueue.resume();

        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.RUNNING);
    }

    @Test
    void shouldMarkQueueAsWaitingLimitAndConfirmation() {
        PromptQueue promptQueue = queue();

        promptQueue.markWaitingLimit();
        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);

        promptQueue.markWaitingConfirmation();
        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.WAITING_CONFIRMATION);
    }

    @Test
    void shouldMarkRunningQueueAsWaitingLimit() {
        PromptQueue promptQueue = queue();
        promptQueue.start();

        promptQueue.markWaitingLimit();

        assertThat(promptQueue.getStatus()).isEqualTo(QueueStatus.WAITING_LIMIT);
    }

    @Test
    void shouldChangeExecutionPolicy() {
        PromptQueue promptQueue = queue();
        QueueExecutionPolicy policy = new QueueExecutionPolicy(
            AutoRunMode.AUTO_RUN,
            5,
            Duration.ZERO,
            false,
            false,
            null
        );

        promptQueue.changeExecutionPolicy(policy);

        assertThat(promptQueue.getExecutionPolicy()).isEqualTo(policy);
        assertThat(promptQueue.shouldStopOnError()).isFalse();
    }

    @Test
    void shouldRejectStartingDisabledQueue() {
        PromptQueue promptQueue = queue();

        promptQueue.disable();

        assertThatIllegalStateException()
            .isThrownBy(promptQueue::start)
            .withMessage("Disabled queue cannot be started");
    }

    @Test
    void shouldRejectStartingAlreadyRunningQueue() {
        PromptQueue promptQueue = queue();

        promptQueue.start();

        assertThatIllegalStateException()
            .isThrownBy(promptQueue::start)
            .withMessage("Running queue cannot be started again");
    }

    @Test
    void shouldRejectInvalidQueueRestoreTimestamps() {
        Instant createdAt = Instant.parse("2026-01-02T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T00:00:00Z");

        assertThatIllegalArgumentException()
            .isThrownBy(() -> PromptQueue.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Queue",
                QueueStatus.CREATED,
                QueueExecutionPolicy.defaultPolicy(),
                createdAt,
                updatedAt
            ))
            .withMessage("Queue updatedAt must not be before createdAt");
    }

    private PromptQueue queue() {
        return PromptQueue.create(
            UUID.randomUUID(),
            "Backend queue",
            QueueExecutionPolicy.defaultPolicy()
        );
    }
}
