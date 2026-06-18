package com.aiq.domain.queue;

import com.aiq.domain.common.AggregateRoot;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;

public class Prompt extends AggregateRoot {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final int MAX_CONTENT_LENGTH = 50_000;

    @Getter
    private final UUID id;
    @Getter
    private final UUID queueId;
    @Getter
    private final UUID targetAiToolId;
    @Getter
    private String title;
    @Getter
    private String content;
    @Getter
    private int priority;
    @Getter
    private long position;
    @Getter
    private PromptStatus status;
    private final String workingDirectoryOverride;
    @Getter
    private int attemptCount;
    @Getter
    private final int maxAttempts;
    @Getter
    private final Instant createdAt;
    @Getter
    private Instant updatedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private String failureReason;

    private Prompt(
        UUID id,
        UUID queueId,
        UUID targetAiToolId,
        String title,
        String content,
        PromptStatus status,
        int priority,
        long position,
        String workingDirectoryOverride,
        int attemptCount,
        int maxAttempts,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt,
        String failureReason
    ) {
        this.id = Objects.requireNonNull(id, "Prompt id must not be null");
        this.queueId = Objects.requireNonNull(queueId, "Queue id must not be null");
        this.targetAiToolId = Objects.requireNonNull(targetAiToolId, "Target AI tool id must not be null");
        this.title = validateTitle(title);
        this.content = validateContent(content);
        this.status = Objects.requireNonNull(status, "Prompt status must not be null");
        this.priority = validatePriority(priority);
        this.position = validatePosition(position);
        this.workingDirectoryOverride = normalizeWorkingDirectoryOverride(workingDirectoryOverride);
        if (attemptCount < 0) {
            throw new IllegalArgumentException("Attempt count must not be negative");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Max attempts must be positive");
        }
        if (attemptCount > maxAttempts) {
            throw new IllegalArgumentException("Attempt count must not exceed max attempts");
        }
        this.attemptCount = attemptCount;
        this.maxAttempts = maxAttempts;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.failureReason = normalizeFailureReason(failureReason);
        if (updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Prompt updatedAt must not be before createdAt");
        }
        if (startedAt != null && finishedAt != null && finishedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Prompt finishedAt must not be before startedAt");
        }
    }

    public static Prompt createQueued(
        UUID queueId,
        UUID targetAiToolId,
        String title,
        String content,
        int priority,
        long position,
        int maxAttempts,
        String workingDirectoryOverride
    ) {
        Instant now = Instant.now();
        return new Prompt(
            UUID.randomUUID(),
            queueId,
            targetAiToolId,
            title,
            content,
            PromptStatus.QUEUED,
            priority,
            position,
            workingDirectoryOverride,
            0,
            maxAttempts,
            now,
            now,
            null,
            null,
            null
        );
    }

    public static Prompt createDraft(
        UUID queueId,
        UUID targetAiToolId,
        String title,
        String content,
        int priority,
        long position,
        int maxAttempts,
        String workingDirectoryOverride
    ) {
        Instant now = Instant.now();
        return new Prompt(
            UUID.randomUUID(),
            queueId,
            targetAiToolId,
            title,
            content,
            PromptStatus.DRAFT,
            priority,
            position,
            workingDirectoryOverride,
            0,
            maxAttempts,
            now,
            now,
            null,
            null,
            null
        );
    }

    public static Prompt restore(
        UUID id,
        UUID queueId,
        UUID targetAiToolId,
        String title,
        String content,
        PromptStatus status,
        int priority,
        long position,
        String workingDirectoryOverride,
        int attemptCount,
        int maxAttempts,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant finishedAt,
        String failureReason
    ) {
        return new Prompt(
            id,
            queueId,
            targetAiToolId,
            title,
            content,
            status,
            priority,
            position,
            workingDirectoryOverride,
            attemptCount,
            maxAttempts,
            createdAt,
            updatedAt,
            startedAt,
            finishedAt,
            failureReason
        );
    }

    public void enqueue() {
        ensureStatus(PromptStatus.DRAFT, "Prompt can be enqueued only from DRAFT status");
        this.status = PromptStatus.QUEUED;
        this.updatedAt = Instant.now();
    }

    public void markWaitingLimit() {
        ensureStatus(PromptStatus.QUEUED, "Prompt can wait for limit only from QUEUED status");
        this.status = PromptStatus.WAITING_LIMIT;
        this.updatedAt = Instant.now();
    }

    public void markWaitingConfirmation() {
        ensureStatus(PromptStatus.QUEUED, "Prompt can wait for confirmation only from QUEUED status");
        this.status = PromptStatus.WAITING_CONFIRMATION;
        this.updatedAt = Instant.now();
    }

    public void start() {
        if (status != PromptStatus.QUEUED && !(status == PromptStatus.FAILED && canRetry())) {
            throw new IllegalStateException("Prompt can be started only from QUEUED status or retryable FAILED status");
        }
        if (attemptCount >= maxAttempts) {
            throw new IllegalStateException("Prompt has no attempts left");
        }

        attemptCount++;
        this.status = PromptStatus.RUNNING;
        this.startedAt = Instant.now();
        this.finishedAt = null;
        this.failureReason = null;
        this.updatedAt = startedAt;
    }

    public void complete() {
        ensureStatus(PromptStatus.RUNNING, "Prompt can be completed only from RUNNING status");
        this.status = PromptStatus.COMPLETED;
        this.finishedAt = Instant.now();
        this.updatedAt = finishedAt;
    }

    public void fail(String reason) {
        ensureStatus(PromptStatus.RUNNING, "Prompt can fail only from RUNNING status");
        this.status = PromptStatus.FAILED;
        this.failureReason = normalizeFailureReason(reason);
        this.finishedAt = Instant.now();
        this.updatedAt = finishedAt;
    }

    public void retry() {
        ensureStatus(PromptStatus.FAILED, "Prompt can be retried only from FAILED status");
        if (!canRetry()) {
            throw new IllegalStateException("Prompt has no retry attempts left");
        }

        this.status = PromptStatus.QUEUED;
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        ensureNotTerminal("Terminal prompt cannot be cancelled");
        this.status = PromptStatus.CANCELLED;
        this.finishedAt = Instant.now();
        this.updatedAt = finishedAt;
    }

    public void skip() {
        ensureNotTerminal("Terminal prompt cannot be skipped");
        this.status = PromptStatus.SKIPPED;
        this.finishedAt = Instant.now();
        this.updatedAt = finishedAt;
    }

    public void changePriority(int newPriority) {
        ensureEditable();
        this.priority = validatePriority(newPriority);
        this.updatedAt = Instant.now();
    }

    public void changeContent(String newContent) {
        ensureEditable();
        this.content = validateContent(newContent);
        this.updatedAt = Instant.now();
    }

    public void changeTitle(String newTitle) {
        ensureEditable();
        this.title = validateTitle(newTitle);
        this.updatedAt = Instant.now();
    }

    public boolean canRetry() {
        return status == PromptStatus.FAILED && attemptCount < maxAttempts;
    }

    public boolean isQueued() {
        return status == PromptStatus.QUEUED;
    }

    public boolean isTerminal() {
        return status == PromptStatus.COMPLETED
            || status == PromptStatus.CANCELLED
            || status == PromptStatus.SKIPPED;
    }

    public Optional<String> workingDirectoryOverride() {
        return Optional.ofNullable(workingDirectoryOverride);
    }

    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> finishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    private void ensureStatus(PromptStatus expectedStatus, String message) {
        if (status != expectedStatus) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureNotTerminal(String message) {
        if (isTerminal()) {
            throw new IllegalStateException(message);
        }
    }

    private void ensureEditable() {
        if (isTerminal()) {
            throw new IllegalStateException("Terminal prompt cannot be edited");
        }
    }

    private static String normalizeWorkingDirectoryOverride(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Working directory override must not be blank");
        }
        return normalizedValue;
    }

    private static String normalizeFailureReason(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private static String validateTitle(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Prompt title must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Prompt title must not be blank");
        }
        if (normalizedValue.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("Prompt title must be at most 150 characters");
        }

        return normalizedValue;
    }

    private static String validateContent(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Prompt content must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("Prompt content must not be blank");
        }
        if (normalizedValue.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Prompt content must be at most 50000 characters");
        }

        return normalizedValue;
    }

    private static int validatePriority(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Prompt priority must not be negative");
        }

        return value;
    }

    private static long validatePosition(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Prompt position must not be negative");
        }

        return value;
    }
}
