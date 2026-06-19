package com.aiq.application.port.out;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;
import com.aiq.domain.execution.ExecutionResult;
import java.util.Optional;

public interface AiLimitChecker {

    AiLimitCheckResult checkLimit(AiLimitCheckRequest request);

    Optional<AiLimitCheckResult> detectLimit(
        AiLimitCheckRequest request,
        ExecutionResult executionResult
    );
}
