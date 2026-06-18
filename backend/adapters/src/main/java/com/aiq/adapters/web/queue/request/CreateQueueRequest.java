package com.aiq.adapters.web.queue.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "Request for creating a prompt queue")
public record CreateQueueRequest(
    @Schema(description = "Project that owns the queue", example = "c4c2b1b6-f7e8-4465-891f-9641d31f7c52")
    @NotNull(message = "projectId must not be null")
    UUID projectId,

    @Schema(description = "Queue display name", example = "Daily AI tasks", maxLength = 100)
    @NotBlank(message = "Queue name must not be blank")
    @Size(max = 100, message = "Queue name must be at most 100 characters")
    String name,

    @Schema(description = "Queue execution policy")
    @Valid
    @NotNull(message = "executionPolicy must not be null")
    QueueExecutionPolicyRequest executionPolicy
) {
}
