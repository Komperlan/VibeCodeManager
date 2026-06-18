package com.aiq.domain.queue;

import com.aiq.domain.common.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class PromptQueue extends AggregateRoot {

    private static final int MAX_NAME_LENGTH = 100;

    private final UUID id;
    private final UUID projectId;
    private String name;
    private QueueExecutionPolicy executionPolicy;
    private final Instant createdAt;
    private Instant updatedAt;

    private QueueStatus status;

    private PromptQueue(
        UUID id,
        UUID projectId,
        String name,
        QueueStatus status,
        QueueExecutionPolicy executionPolicy,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id, "Prompt queue id must not be null");
        this.projectId = Objects.requireNonNull(projectId, "Project id must not be null");
        this.name = validateName(name);
        this.status = Objects.requireNonNull(status, "Queue status must not be null");
        this.executionPolicy = Objects.requireNonNull(executionPolicy, "Queue execution policy must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Queue updatedAt must not be before createdAt");
        }
    }

    public static PromptQueue create(UUID projectId, String name, QueueExecutionPolicy policy) {
        Instant now = Instant.now();
        return new PromptQueue(
            UUID.randomUUID(),
            projectId,
            name,
            QueueStatus.CREATED,
            policy == null ? QueueExecutionPolicy.defaultPolicy() : policy,
            now,
            now
        );
    }

    public static PromptQueue restore(
        UUID id,
        UUID projectId,
        String name,
        QueueStatus status,
        QueueExecutionPolicy executionPolicy,
        Instant createdAt,
        Instant updatedAt
    ) {
        return new PromptQueue(
            id,
            projectId,
            name,
            status,
            executionPolicy,
            createdAt,
            updatedAt
        );
    }

    public void start() {
        if (status == QueueStatus.RUNNING) {
            throw new IllegalStateException("Running queue cannot be started again");
        }
        if (status == QueueStatus.PAUSED) {
            throw new IllegalStateException("Paused queue must be resumed before running");
        }
        if (status == QueueStatus.DISABLED) {
            throw new IllegalStateException("Disabled queue cannot be started");
        }
        if (status == QueueStatus.COMPLETED) {
            throw new IllegalStateException("Completed queue cannot be started");
        }

        changeStatus(QueueStatus.RUNNING);
    }

    public void pause() {
        ensureStatus(QueueStatus.RUNNING, "Queue can be paused only from RUNNING status");
        changeStatus(QueueStatus.PAUSED);
    }

    public void resume() {
        if (status != QueueStatus.PAUSED && status != QueueStatus.STOPPED) {
            throw new IllegalStateException("Queue can be resumed only from PAUSED or STOPPED status");
        }

        changeStatus(QueueStatus.RUNNING);
    }

    public void stop(String reason) {
        if (status == QueueStatus.COMPLETED) {
            throw new IllegalStateException("Completed queue cannot be stopped");
        }
        if (status == QueueStatus.DISABLED) {
            throw new IllegalStateException("Disabled queue cannot be stopped");
        }

        changeStatus(QueueStatus.STOPPED);
    }

    public void markWaitingLimit() {
        ensureCanWait("Queue cannot wait for limit from " + status + " status");
        changeStatus(QueueStatus.WAITING_LIMIT);
    }

    public void markWaitingConfirmation() {
        ensureCanWait("Queue cannot wait for confirmation from " + status + " status");
        changeStatus(QueueStatus.WAITING_CONFIRMATION);
    }

    public void complete() {
        if (status == QueueStatus.DISABLED) {
            throw new IllegalStateException("Disabled queue cannot be completed");
        }

        changeStatus(QueueStatus.COMPLETED);
    }

    public void disable() {
        changeStatus(QueueStatus.DISABLED);
    }

    public void enable() {
        ensureStatus(QueueStatus.DISABLED, "Only disabled queue can be enabled");
        changeStatus(QueueStatus.CREATED);
    }

    public void changeExecutionPolicy(QueueExecutionPolicy policy) {
        this.executionPolicy = Objects.requireNonNull(policy, "Queue execution policy must not be null");
        this.updatedAt = Instant.now();
    }

    public boolean canRun() {
        return status != QueueStatus.RUNNING
            && status != QueueStatus.PAUSED
            && status != QueueStatus.DISABLED
            && status != QueueStatus.COMPLETED;
    }

    public boolean shouldStopOnError() {
        return executionPolicy.stopOnError();
    }

    private void ensureCanWait(String message) {
        if (status == QueueStatus.PAUSED || status == QueueStatus.DISABLED || status == QueueStatus.COMPLETED) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureStatus(QueueStatus expectedStatus, String message) {
        if (status != expectedStatus) {
            throw new IllegalStateException(message);
        }
    }

    private void changeStatus(QueueStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    private static String validateName(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Queue name must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Queue name must not be blank");
        }
        if (normalizedValue.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Queue name must be at most 100 characters");
        }

        return normalizedValue;
    }
}
