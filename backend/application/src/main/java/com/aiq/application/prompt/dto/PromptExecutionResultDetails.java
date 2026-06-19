package com.aiq.application.prompt.dto;

import com.aiq.domain.execution.ExecutionStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public record PromptExecutionResultDetails(
    UUID executionId,
    ExecutionStatus status,
    String command,
    Optional<Integer> exitCode,
    Optional<String> responseText,
    Optional<String> stderr,
    Optional<String> rawOutput,
    Optional<String> errorMessage,
    Optional<Instant> startedAt,
    Optional<Instant> finishedAt,
    Optional<Long> durationMillis
) {
}
