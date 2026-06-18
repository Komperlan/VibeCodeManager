package com.aiq.infrastructure.limit;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.application.port.out.AiLimitChecker;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "aiq.limit.fake.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = "aiq.executor.codex.enabled", havingValue = "false", matchIfMissing = true)
public class FakeAiLimitChecker implements AiLimitChecker {

    @Override
    public AiLimitCheckResult checkLimit(AiLimitCheckRequest request) {
        Objects.requireNonNull(request, "AI limit check request must not be null");
        return AiLimitCheckResult.available();
    }
}
