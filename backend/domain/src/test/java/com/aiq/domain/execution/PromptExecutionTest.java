package com.aiq.domain.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptExecutionTest {

    @Test
    void shouldCompleteSuccessfulExecution() {
        PromptExecution execution = execution();

        execution.start();
        execution.complete(new ExecutionResult(
            0,
            "ok",
            "",
            "ok",
            null
        ));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.COMPLETED);
        assertThat(execution.isFinished()).isTrue();
        assertThat(execution.result()).isPresent();
        assertThat(execution.finishedAt()).isPresent();
        assertThat(execution.duration()).isPresent();
    }

    @Test
    void shouldFailExecutionWhenExitCodeIsNonZero() {
        PromptExecution execution = execution();

        execution.start();
        execution.complete(new ExecutionResult(
            1,
            "",
            "failed",
            "failed",
            "failed"
        ));

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
    }

    @Test
    void shouldTimeoutRunningExecution() {
        PromptExecution execution = execution();

        execution.start();
        execution.timeout();

        assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.TIMEOUT);
        assertThat(execution.isFinished()).isTrue();
    }

    @Test
    void shouldRejectCompletingExecutionBeforeStart() {
        PromptExecution execution = execution();

        assertThatIllegalStateException()
            .isThrownBy(() -> execution.complete(new ExecutionResult(
                0,
                "",
                "",
                "",
                null
            )))
            .withMessage("Execution can be completed only from RUNNING status");
    }

    private PromptExecution execution() {
        return PromptExecution.create(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "codex run"
        );
    }
}
