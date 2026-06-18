package com.aiq.application.runner;

import java.util.Objects;
import java.util.UUID;

public record PromptExecutionRequest(
    UUID promptId,
    UUID aiToolId,
    String title,
    String content,
    String workingDirectoryOverride
) {

    public PromptExecutionRequest {
        Objects.requireNonNull(promptId, "Prompt id must not be null");
        Objects.requireNonNull(aiToolId, "AI tool id must not be null");
        Objects.requireNonNull(title, "Prompt title must not be null");
        Objects.requireNonNull(content, "Prompt content must not be null");
    }
}
