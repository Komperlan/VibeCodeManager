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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aiq.executor.codex.enabled", havingValue = "true")
@Slf4j
public class CodexCliPromptExecutor implements PromptExecutor {

    private static final int MAX_LOG_ERROR_LENGTH = 500;

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
        log.info(
            "Starting Codex CLI prompt {} in working directory {} with command {}",
            request.promptId(),
            command.workingDirectory(),
            command.arguments()
        );

        ProcessRunResult result = processRunner.run(command);
        logProcessResult(request, result);

        return outputParser.parse(result);
    }

    private void logProcessResult(PromptExecutionRequest request, ProcessRunResult result) {
        String errorMessage = result.errorMessage();
        if (result.exitCode() == 0 && errorMessage == null && !result.timedOut()) {
            log.info(
                "Finished Codex CLI prompt {} successfully: exitCode={}, duration={}, stdoutBytes={}, stderrBytes={}",
                request.promptId(),
                result.exitCode(),
                result.duration(),
                byteCount(result.stdout()),
                byteCount(result.stderr())
            );
            return;
        }

        log.warn(
            "Finished Codex CLI prompt {} with failure: exitCode={}, duration={}, timedOut={}, stdoutBytes={}, stderrBytes={}, error={}",
            request.promptId(),
            result.exitCode(),
            result.duration(),
            result.timedOut(),
            byteCount(result.stdout()),
            byteCount(result.stderr()),
            shorten(firstNonBlank(errorMessage, result.stderr(), result.stdout()))
        );
    }

    private int byteCount(String value) {
        return value == null ? 0 : value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    private String shorten(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalizedValue = value.replaceAll("\\s+", " ").trim();
        if (normalizedValue.length() <= MAX_LOG_ERROR_LENGTH) {
            return normalizedValue;
        }

        return normalizedValue.substring(0, MAX_LOG_ERROR_LENGTH) + "...";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }
}
