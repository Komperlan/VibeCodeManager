package com.aiq.infrastructure.limit.codex;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.application.limit.AiLimitStatus;
import com.aiq.infrastructure.executor.request.ProcessRunResult;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class CodexLimitOutputParserTest {

    private final CodexLimitOutputParser parser = new CodexLimitOutputParser();

    @Test
    void shouldReturnAvailableForSuccessfulProbe() {
        var result = parser.parse(processResult(0, "AVAILABLE", "", null, false));

        assertThat(result.status()).isEqualTo(AiLimitStatus.AVAILABLE);
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void shouldRecognizeRateLimitInStdout() {
        var result = parser.parse(processResult(1, "rate limit reached", "", null, false));

        assertThat(result.status()).isEqualTo(AiLimitStatus.LIMIT_REACHED);
        assertThat(result.reason()).contains("rate limit reached");
    }

    @Test
    void shouldRecognizeTooManyRequestsInStderr() {
        var result = parser.parse(processResult(1, "", "Too Many Requests: 429", null, false));

        assertThat(result.status()).isEqualTo(AiLimitStatus.LIMIT_REACHED);
        assertThat(result.reason()).contains("Too Many Requests");
    }

    @Test
    void shouldRecognizeQuotaInRunnerErrorMessage() {
        var result = parser.parse(processResult(-1, "", "", "quota exceeded", false));

        assertThat(result.status()).isEqualTo(AiLimitStatus.LIMIT_REACHED);
        assertThat(result.reason()).contains("quota exceeded");
    }

    @Test
    void shouldReturnErrorForTimedOutProbe() {
        var result = parser.parse(processResult(-1, "", "", "Process timed out after PT30S", true));

        assertThat(result.status()).isEqualTo(AiLimitStatus.ERROR);
        assertThat(result.reason()).contains("Process timed out");
    }

    @Test
    void shouldReturnErrorForUnknownNonZeroExit() {
        var result = parser.parse(processResult(2, "", "auth failed", null, false));

        assertThat(result.status()).isEqualTo(AiLimitStatus.ERROR);
        assertThat(result.reason()).isEqualTo("auth failed");
    }

    private ProcessRunResult processResult(
        int exitCode,
        String stdout,
        String stderr,
        String errorMessage,
        boolean timedOut
    ) {
        String rawOutput = stdout.isBlank() ? stderr : stdout;
        return new ProcessRunResult(
            exitCode,
            stdout,
            stderr,
            rawOutput,
            Duration.ofMillis(100),
            timedOut,
            errorMessage
        );
    }
}
