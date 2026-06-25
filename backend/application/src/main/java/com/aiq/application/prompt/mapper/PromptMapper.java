package com.aiq.application.prompt.mapper;

import com.aiq.application.prompt.dto.AddPromptResult;
import com.aiq.application.prompt.dto.PromptDetails;
import com.aiq.application.prompt.dto.PromptExecutionResultDetails;
import com.aiq.application.prompt.dto.PromptSummary;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.domain.queue.Prompt;
import java.util.Optional;

public final class PromptMapper {

    private PromptMapper() {
    }

    public static AddPromptResult toAddPromptResult(Prompt prompt) {
        return new AddPromptResult(prompt.getId());
    }

    public static PromptDetails toDetails(Prompt prompt) {
        return toDetails(prompt, Optional.empty());
    }

    public static PromptDetails toDetails(Prompt prompt, Optional<PromptExecution> lastExecution) {
        return new PromptDetails(
            prompt.getId(),
            prompt.getQueueId(),
            prompt.getTargetAiToolId(),
            prompt.getTitle(),
            prompt.getContent(),
            prompt.getStatus(),
            prompt.getPriority(),
            prompt.getPosition(),
            prompt.workingDirectoryOverride(),
            prompt.getAttemptCount(),
            prompt.getMaxAttempts(),
            prompt.getCreatedAt(),
            prompt.getUpdatedAt(),
            prompt.startedAt(),
            prompt.finishedAt(),
            prompt.failureReason(),
            lastExecution.map(PromptMapper::toExecutionResultDetails)
        );
    }

    public static PromptSummary toSummary(Prompt prompt) {
        return new PromptSummary(
            prompt.getId(),
            prompt.getTitle(),
            prompt.getStatus(),
            prompt.getPriority(),
            prompt.getPosition()
        );
    }

    private static PromptExecutionResultDetails toExecutionResultDetails(PromptExecution execution) {
        Optional<ExecutionResult> result = execution.result();

        return new PromptExecutionResultDetails(
            execution.getId(),
            execution.getStatus(),
            execution.getCommand(),
            result.map(ExecutionResult::exitCode),
            result.map(ExecutionResult::stdout).filter(value -> !value.isBlank()),
            result.map(ExecutionResult::stderr).filter(value -> !value.isBlank()),
            result.map(ExecutionResult::rawOutput).filter(value -> !value.isBlank()),
            result.map(ExecutionResult::errorMessage).filter(value -> value != null && !value.isBlank()),
            result.map(ExecutionResult::externalSessionId).filter(value -> value != null && !value.isBlank()),
            execution.startedAt(),
            execution.finishedAt(),
            execution.duration().map(java.time.Duration::toMillis)
        );
    }
}
