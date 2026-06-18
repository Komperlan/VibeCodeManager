package com.aiq.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.domain.execution.ExecutionResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FakePromptExecutorTest {

    private final FakePromptExecutor executor = new FakePromptExecutor();

    @Test
    void shouldBuildCommandWithPromptAndToolIds() {
        PromptExecutionRequest request = request("Implement queue runner");

        String command = executor.buildCommand(request);

        assertThat(command)
            .startsWith("fake-executor")
            .contains(request.aiToolId().toString())
            .contains(request.promptId().toString())
            .contains("/tmp/project");
    }

    @Test
    void shouldExecutePromptSuccessfully() {
        PromptExecutionRequest request = request("Implement queue runner");

        ExecutionResult result = executor.execute(request);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).contains(request.title());
        assertThat(result.stderr()).isEmpty();
        assertThat(result.rawOutput()).contains(request.content());
        assertThat(result.errorMessage()).isNull();
    }

    @Test
    void shouldSimulateFailureWhenContentContainsFailureMarker() {
        PromptExecutionRequest request = request("Implement queue runner " + FakePromptExecutor.FAILURE_MARKER);

        ExecutionResult result = executor.execute(request);

        assertThat(result.isFailed()).isTrue();
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.stderr()).contains("Fake execution failed");
        assertThat(result.errorMessage()).contains(request.promptId().toString());
    }

    private PromptExecutionRequest request(String content) {
        return new PromptExecutionRequest(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "Backend prompt",
            content,
            "/tmp/project"
        );
    }
}
