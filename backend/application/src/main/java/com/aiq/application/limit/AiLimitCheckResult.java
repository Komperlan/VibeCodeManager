package com.aiq.application.limit;

import java.time.Instant;
import java.util.Objects;

public record AiLimitCheckResult(
    AiLimitStatus status,
    String reason,
    Instant resetAt
) {

    public AiLimitCheckResult {
        Objects.requireNonNull(status, "AI limit status must not be null");
        reason = normalizeReason(status, reason);
    }

    public static AiLimitCheckResult available() {
        return new AiLimitCheckResult(AiLimitStatus.AVAILABLE, "AI tool limit is available", null);
    }

    public static AiLimitCheckResult limitReached(String reason, Instant resetAt) {
        return new AiLimitCheckResult(AiLimitStatus.LIMIT_REACHED, reason, resetAt);
    }

    public static AiLimitCheckResult unknown(String reason) {
        return new AiLimitCheckResult(AiLimitStatus.UNKNOWN, reason, null);
    }

    public static AiLimitCheckResult error(String reason) {
        return new AiLimitCheckResult(AiLimitStatus.ERROR, reason, null);
    }

    public boolean isAvailable() {
        return status == AiLimitStatus.AVAILABLE;
    }

    private static String normalizeReason(AiLimitStatus status, String value) {
        if (value != null && !value.isBlank()) {
            return value.trim();
        }

        return switch (status) {
            case AVAILABLE -> "AI tool limit is available";
            case LIMIT_REACHED -> "AI tool limit reached";
            case UNKNOWN -> "AI tool limit status is unknown";
            case ERROR -> "AI tool limit check failed";
        };
    }
}
