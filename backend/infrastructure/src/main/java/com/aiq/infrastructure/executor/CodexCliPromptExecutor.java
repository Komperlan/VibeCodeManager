package com.aiq.infrastructure.executor;

import com.aiq.application.port.out.PromptExecutor;
import com.aiq.application.runner.PromptExecutionRequest;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.codex.CodexCommandBuilder;
import com.aiq.infrastructure.executor.codex.CodexOutputParser;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aiq.executor.codex.enabled", havingValue = "true")
public class CodexCliPromptExecutor implements PromptExecutor {

    private final ProcessRunner processRunner;
    private final CodexCommandBuilder commandBuilder;
    private final CodexOutputParser outputParser;

    @Override
    public String buildCommand(PromptExecutionRequest request) {
        Objects.requireNonNull(request, "Prompt execution request must not be null");

        return commandBuilder.buildSafeCommand(request);
    }

    @Override
    public ExecutionResult execute(PromptExecutionRequest request) {
        Objects.requireNonNull(request, "Prompt execution request must not be null");

        ProcessCommand command = commandBuilder.build(request);
        ProcessRunResult result = processRunner.run(command);

        return outputParser.parse(result);
    }
}
