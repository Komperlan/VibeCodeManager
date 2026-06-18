package com.aiq.infrastructure.limit.codex;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.application.port.out.AiLimitChecker;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.infrastructure.executor.ProcessRunner;
import com.aiq.infrastructure.executor.request.ProcessCommand;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "aiq.executor.codex.enabled", havingValue = "true")
public class CodexCliLimitChecker implements AiLimitChecker {

    private final ProcessRunner processRunner;
    private final CodexLimitCheckCommandBuilder commandBuilder;
    private final CodexLimitOutputParser outputParser;

    @Override
    public AiLimitCheckResult checkLimit(AiLimitCheckRequest request) {
        Objects.requireNonNull(request, "AI limit check request must not be null");
        if (request.aiToolType() != AiToolType.CODEX) {
            return AiLimitCheckResult.error("Codex limit checker supports only CODEX tools: " + request.aiToolType());
        }

        ProcessCommand command = commandBuilder.build(request);
        ProcessRunResult result = processRunner.run(command);
        return outputParser.parse(result);
    }
}
