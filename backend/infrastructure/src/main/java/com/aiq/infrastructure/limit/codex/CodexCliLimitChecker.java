package com.aiq.infrastructure.limit.codex;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.application.limit.AiLimitStatus;
import com.aiq.application.port.out.AiLimitChecker;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.ProcessRunner;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aiq.executor.codex.enabled", havingValue = "true")
@Slf4j
public class CodexCliLimitChecker implements AiLimitChecker {

    private final ProcessRunner processRunner;
    private final CodexLimitCheckCommandBuilder commandBuilder;
    private final CodexLimitOutputParser outputParser;
    private final CodexLimitCheckerProperties properties;

    @Override
    public AiLimitCheckResult checkLimit(AiLimitCheckRequest request) {
        Objects.requireNonNull(request, "AI limit check request must not be null");
        if (request.aiToolType() != AiToolType.CODEX) {
            return AiLimitCheckResult.error("Codex limit checker supports only CODEX tools: " + request.aiToolType());
        }

        ProcessCommand command = commandBuilder.build(request);
        ProcessRunResult result = processRunner.run(command);
        AiLimitCheckResult limitCheckResult = outputParser.parse(result);
        if (shouldFailOpen(limitCheckResult)) {
            log.warn(
                "Codex limit probe failed open for tool {} in working directory {}: {}",
                request.aiToolId(),
                request.workingDirectory(),
                limitCheckResult.reason()
            );
            return AiLimitCheckResult.available();
        }

        return limitCheckResult;
    }

    @Override
    public Optional<AiLimitCheckResult> detectLimit(
        AiLimitCheckRequest request,
        ExecutionResult executionResult
    ) {
        Objects.requireNonNull(request, "AI limit check request must not be null");
        Objects.requireNonNull(executionResult, "Execution result must not be null");
        if (request.aiToolType() != AiToolType.CODEX) {
            return Optional.empty();
        }

        return outputParser.detectLimit(executionResult);
    }

    private boolean shouldFailOpen(AiLimitCheckResult result) {
        return properties.isFailOpenOnError()
            && (result.status() == AiLimitStatus.ERROR || result.status() == AiLimitStatus.UNKNOWN);
    }
}
