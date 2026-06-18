package com.aiq.application.limit;

import com.aiq.domain.aitool.AiToolType;
import java.util.Objects;
import java.util.UUID;

public record AiLimitCheckRequest(
    UUID aiToolId,
    AiToolType aiToolType,
    String executablePath,
    String workingDirectory
) {

    public AiLimitCheckRequest {
        Objects.requireNonNull(aiToolId, "AI tool id must not be null");
        Objects.requireNonNull(aiToolType, "AI tool type must not be null");
        executablePath = normalizeExecutablePath(executablePath);
        workingDirectory = normalizeWorkingDirectory(workingDirectory);
    }

    private static String normalizeExecutablePath(String value) {
        if (value == null) {
            throw new IllegalArgumentException("AI tool executable path must not be null");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException("AI tool executable path must not be blank");
        }

        return normalizedValue;
    }

    private static String normalizeWorkingDirectory(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
