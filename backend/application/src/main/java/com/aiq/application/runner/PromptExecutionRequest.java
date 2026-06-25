package com.aiq.application.runner;

import java.util.Objects;
import java.util.UUID;

public record PromptExecutionRequest(
    UUID promptId,
    UUID aiToolId,
    String title,
    String content,
    String workingDirectoryOverride,
    String codexSessionId
) {

    public PromptExecutionRequest(
        UUID promptId,
        UUID aiToolId,
        String title,
        String content,
        String workingDirectoryOverride
    ) {
        this(promptId, aiToolId, title, content, workingDirectoryOverride, null);
    }

    public PromptExecutionRequest {
        Objects.requireNonNull(promptId, "Prompt id must not be null");
        Objects.requireNonNull(aiToolId, "AI tool id must not be null");
        Objects.requireNonNull(title, "Prompt title must not be null");
        Objects.requireNonNull(content, "Prompt content must not be null");
        codexSessionId = normalizeCodexSessionId(codexSessionId);
    }

    public boolean hasCodexSessionId() {
        return codexSessionId != null;
    }

    private static String normalizeCodexSessionId(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }
}
