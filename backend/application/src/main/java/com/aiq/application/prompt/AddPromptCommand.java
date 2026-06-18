package com.aiq.application.prompt;

import java.util.UUID;

public record AddPromptCommand(
    UUID queueId,
    UUID targetAiToolId,
    String title,
    String content,
    int priority,
    int maxAttempts,
    String workingDirectoryOverride
) {
}
