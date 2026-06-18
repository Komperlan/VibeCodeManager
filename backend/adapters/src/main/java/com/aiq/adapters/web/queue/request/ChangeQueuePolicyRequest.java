package com.aiq.adapters.web.queue.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request for replacing queue execution policy")
public record ChangeQueuePolicyRequest(
    @Schema(description = "New queue execution policy")
    @Valid
    @NotNull(message = "executionPolicy must not be null")
    QueueExecutionPolicyRequest executionPolicy
) {
}
