package com.aiq.infrastructure.limit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitStatus;
import com.aiq.domain.aitool.AiToolType;
import com.aiq.domain.execution.ExecutionResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FakeAiLimitCheckerTest {

    private final FakeAiLimitChecker checker = new FakeAiLimitChecker();

    @Test
    void shouldAlwaysReturnAvailable() {
        AiLimitCheckRequest request = new AiLimitCheckRequest(
            UUID.randomUUID(),
            AiToolType.FAKE,
            "fake-executor",
            "/tmp/project"
        );

        var result = checker.checkLimit(request);

        assertThat(result.status()).isEqualTo(AiLimitStatus.AVAILABLE);
        assertThat(result.isAvailable()).isTrue();
    }

    @Test
    void shouldNeverDetectLimitInFakeExecutionResult() {
        AiLimitCheckRequest request = new AiLimitCheckRequest(
            UUID.randomUUID(),
            AiToolType.FAKE,
            "fake-executor",
            "/tmp/project"
        );

        var result = checker.detectLimit(
            request,
            new ExecutionResult(1, "rate limit", "", "rate limit", "rate limit")
        );

        assertThat(result).isEmpty();
    }
}
