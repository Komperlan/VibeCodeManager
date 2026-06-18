package com.aiq.domain.execution;

import com.aiq.domain.common.AggregateRoot;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;

public class PromptExecution extends AggregateRoot {

    @Getter
    private final UUID id;
    @Getter
    private final UUID promptId;
    @Getter
    private final UUID aiToolId;
    @Getter
    private ExecutionStatus status;
    @Getter
    private final String command;
    private ExecutionResult result;
    private Instant startedAt;
    private Instant finishedAt;
    private Duration duration;

    private PromptExecution(
        UUID id,
        UUID promptId,
        UUID aiToolId,
        ExecutionStatus status,
        String command,
        ExecutionResult result,
        Instant startedAt,
        Instant finishedAt,
        Duration duration
    ) {
        this.id = Objects.requireNonNull(id, "Prompt execution id must not be null");
        this.promptId = Objects.requireNonNull(promptId, "Prompt id must not be null");
        this.aiToolId = Objects.requireNonNull(aiToolId, "AI tool id must not be null");
        this.status = Objects.requireNonNull(status, "Execution status must not be null");
        this.command = validateCommand(command);
        this.result = result;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.duration = duration;
        if (startedAt != null && finishedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Execution finishedAt must not be before startedAt");
        }
        if (duration != null && duration.isNegative()) {
            throw new IllegalArgumentException("Execution duration must not be negative");
        }
    }

    public static PromptExecution create(UUID promptId, UUID aiToolId, String command) {
        return new PromptExecution(
            UUID.randomUUID(),
            promptId,
            aiToolId,
            ExecutionStatus.CREATED,
            command,
            null,
            null,
            null,
            null
        );
    }

    public static PromptExecution restore(
        UUID id,
        UUID promptId,
        UUID aiToolId,
        ExecutionStatus status,
        String command,
        ExecutionResult result,
        Instant startedAt,
        Instant finishedAt,
        Duration duration
    ) {
        return new PromptExecution(
            id,
            promptId,
            aiToolId,
            status,
            command,
            result,
            startedAt,
            finishedAt,
            duration
        );
    }

    public void start() {
        ensureStatus(ExecutionStatus.CREATED, "Execution can be started only from CREATED status");
        this.status = ExecutionStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void complete(ExecutionResult executionResult) {
        ensureStatus(ExecutionStatus.RUNNING, "Execution can be completed only from RUNNING status");
        this.result = Objects.requireNonNull(executionResult, "Execution result must not be null");
        this.status = executionResult.isSuccessful() ? ExecutionStatus.COMPLETED : ExecutionStatus.FAILED;
        markFinished();
    }

    public void cancel() {
        if (isFinished()) {
            throw new IllegalStateException("Finished execution cannot be cancelled");
        }

        this.status = ExecutionStatus.CANCELLED;
        markFinished();
    }

    public void timeout() {
        ensureStatus(ExecutionStatus.RUNNING, "Execution can time out only from RUNNING status");
        this.status = ExecutionStatus.TIMEOUT;
        markFinished();
    }

    public boolean isFinished() {
        return status == ExecutionStatus.COMPLETED
            || status == ExecutionStatus.FAILED
            || status == ExecutionStatus.CANCELLED
            || status == ExecutionStatus.TIMEOUT;
    }

    public Optional<ExecutionResult> result() {
        return Optional.ofNullable(result);
    }

    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    private void markFinished() {
        this.finishedAt = Instant.now();
        if (startedAt != null) {
            this.duration = Duration.between(startedAt, finishedAt);
        }
    }

    private void ensureStatus(ExecutionStatus expectedStatus, String message) {
        if (status != expectedStatus) {
            throw new IllegalStateException(message);
        }
    }

    private static String validateCommand(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Execution command must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Execution command must not be blank");
        }

        return normalizedValue;
    }
}
