package com.aiq.infrastructure.limit.codex;

import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.domain.execution.ExecutionResult;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CodexLimitOutputParser {

    private static final List<String> LIMIT_PATTERNS = List.of(
        "rate limit",
        "usage limit",
        "quota",
        "limit reached",
        "too many requests",
        "429"
    );

    public AiLimitCheckResult parse(ProcessRunResult result) {
        Objects.requireNonNull(result, "Process run result must not be null");

        String output = combinedOutput(result).toLowerCase();
        if (containsLimitPattern(output)) {
            return AiLimitCheckResult.limitReached(limitReason(result), null);
        }

        if (result.exitCode() == 0 && !result.timedOut() && result.errorMessage() == null) {
            return AiLimitCheckResult.available();
        }

        if (result.timedOut()) {
            return AiLimitCheckResult.error(
                firstNonBlank(result.errorMessage(), "Codex limit probe timed out")
            );
        }

        if (result.errorMessage() != null && !result.errorMessage().isBlank()) {
            return AiLimitCheckResult.error(result.errorMessage());
        }

        return AiLimitCheckResult.error(
            firstNonBlank(
                result.stderr(),
                result.stdout(),
                "Codex limit probe failed with exit code " + result.exitCode()
            )
        );
    }

    public Optional<AiLimitCheckResult> detectLimit(ExecutionResult result) {
        Objects.requireNonNull(result, "Execution result must not be null");

        String output = String.join(
            System.lineSeparator(),
            result.stdout(),
            result.stderr(),
            result.rawOutput(),
            result.errorMessage() == null ? "" : result.errorMessage()
        ).toLowerCase();
        if (!containsLimitPattern(output)) {
            return Optional.empty();
        }

        return Optional.of(AiLimitCheckResult.limitReached(
            firstNonBlank(
                result.errorMessage(),
                result.stderr(),
                result.stdout(),
                result.rawOutput(),
                "Codex CLI limit reached"
            ),
            null
        ));
    }

    private boolean containsLimitPattern(String output) {
        return LIMIT_PATTERNS.stream().anyMatch(output::contains);
    }

    private String combinedOutput(ProcessRunResult result) {
        return String.join(
            System.lineSeparator(),
            result.stdout(),
            result.stderr(),
            result.rawOutput(),
            result.errorMessage() == null ? "" : result.errorMessage()
        );
    }

    private String limitReason(ProcessRunResult result) {
        return firstNonBlank(
            result.errorMessage(),
            result.stderr(),
            result.stdout(),
            result.rawOutput(),
            "Codex CLI limit reached"
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }
}
