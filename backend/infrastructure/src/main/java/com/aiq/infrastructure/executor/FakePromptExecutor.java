package com.aiq.infrastructure.executor;

import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.aiq.application.port.out.PromptExecutor;
import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.domain.execution.ExecutionResult;

@Component
@ConditionalOnProperty(name = "aiq.executor.fake.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "aiq.executor.codex.enabled", havingValue = "false", matchIfMissing = true)
public class FakePromptExecutor implements PromptExecutor {

    public static final String FAILURE_MARKER = "[fake-fail]";

    @Override
    public String buildCommand(PromptExecutionRequest request) {
        Objects.requireNonNull(request, "Prompt execution request must not be null");

        String command = "fake-executor --ai-tool-id " + request.aiToolId()
            + " --prompt-id " + request.promptId();
        if (request.workingDirectoryOverride() != null) {
            command += " --workdir " + request.workingDirectoryOverride();
        }

        return command;
    }

    @Override
    public ExecutionResult execute(PromptExecutionRequest request) {
        Objects.requireNonNull(request, "Prompt execution request must not be null");

        if (request.content().contains(FAILURE_MARKER)) {
            String error = "Fake execution failed for prompt " + request.promptId();
            return new ExecutionResult(
                1,
                "",
                error,
                error,
                error
            );
        }

        String stdout = "Fake executed prompt: " + request.title();
        String rawOutput = stdout + System.lineSeparator() + request.content();
        return new ExecutionResult(
            0,
            stdout,
            "",
            rawOutput,
            null
        );
    }
}
