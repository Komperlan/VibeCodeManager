package com.aiq.adapters.web.prompt.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Request for adding a queued or draft prompt")
public record AddPromptRequest(
    @Schema(description = "Queue that owns the prompt", example = "4d7e61ac-68c7-43fb-9bd7-8ce7e2ac6a10")
    @NotNull(message = "queueId must not be null")
    UUID queueId,

    @Schema(description = "AI tool that should execute the prompt", example = "d47f57db-9f12-48cb-a7f2-6b0e37bf3a18")
    @NotNull(message = "targetAiToolId must not be null")
    UUID targetAiToolId,

    @Schema(description = "Short prompt title", example = "Fix failing Maven tests", maxLength = 150)
    @NotBlank(message = "title must not be blank")
    @Size(max = 150, message = "title must be at most 150 characters")
    String title,

    @Schema(description = "Prompt body sent to the AI tool", example = "Run the test suite and fix compilation errors.", maxLength = 50_000)
    @NotBlank(message = "content must not be blank")
    @Size(max = 50_000, message = "content must be at most 50000 characters")
    String content,

    @Schema(description = "Higher value means higher execution priority", example = "10", minimum = "0")
    @PositiveOrZero(message = "priority must be positive or 0")
    int priority,

    @Schema(description = "Maximum execution attempts for this prompt", example = "3", minimum = "1")
    @Positive(message = "maxAttempts must be positive")
    int maxAttempts,

    @Schema(description = "Optional working directory override for this prompt", example = "/home/user/projects/vibe-code-manager")
    String workingDirectoryOverride
) {

    @AssertTrue(message = "workingDirectoryOverride must not be blank")
    public boolean isWorkingDirectoryOverrideValid() {
        return workingDirectoryOverride == null || !workingDirectoryOverride.isBlank();
    }
}
