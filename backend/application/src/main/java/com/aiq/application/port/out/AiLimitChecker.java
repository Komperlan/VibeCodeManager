package com.aiq.application.port.out;

import com.aiq.application.limit.AiLimitCheckRequest;
import com.aiq.application.limit.AiLimitCheckResult;

public interface AiLimitChecker {

    AiLimitCheckResult checkLimit(AiLimitCheckRequest request);
}
