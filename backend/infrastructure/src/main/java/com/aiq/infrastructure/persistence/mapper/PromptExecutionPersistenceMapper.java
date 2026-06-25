package com.aiq.infrastructure.persistence.mapper;

import com.aiq.domain.execution.ExecutionResult;
import com.aiq.domain.execution.PromptExecution;
import com.aiq.infrastructure.persistence.entity.PromptExecutionJpaEntity;
import java.time.Duration;

public final class PromptExecutionPersistenceMapper {

    private PromptExecutionPersistenceMapper() {
    }

    public static PromptExecutionJpaEntity toEntity(PromptExecution execution) {
        ExecutionResult result = execution.result().orElse(null);

        return new PromptExecutionJpaEntity(
            execution.getId(),
            execution.getPromptId(),
            execution.getAiToolId(),
            execution.getStatus(),
            execution.getCommand(),
            result == null ? null : result.exitCode(),
            result == null ? null : result.stdout(),
            result == null ? null : result.stderr(),
            result == null ? null : result.rawOutput(),
            result == null ? null : result.errorMessage(),
            result == null ? null : result.externalSessionId(),
            execution.startedAt().orElse(null),
            execution.finishedAt().orElse(null),
            execution.duration().map(Duration::toMillis).orElse(null)
        );
    }

    public static PromptExecution toDomain(PromptExecutionJpaEntity entity) {
        ExecutionResult result = entity.getResultExitCode() == null
            ? null
            : new ExecutionResult(
                entity.getResultExitCode(),
                entity.getResultStdout(),
                entity.getResultStderr(),
                entity.getResultRawOutput(),
                entity.getResultErrorMessage(),
                entity.getExternalSessionId()
            );

        Duration duration = entity.getDurationMillis() == null
            ? null
            : Duration.ofMillis(entity.getDurationMillis());

        return PromptExecution.restore(
            entity.getId(),
            entity.getPromptId(),
            entity.getAiToolId(),
            entity.getStatus(),
            entity.getCommand(),
            result,
            entity.getStartedAt(),
            entity.getFinishedAt(),
            duration
        );
    }
}
