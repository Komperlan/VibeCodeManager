package com.aiq.adapters.web.runner.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request for running a queue")
public record RunQueueRequest(
    @Schema(description = "Requested maximum prompts to execute", example = "3", minimum = "1")
    @Positive(message = "maxPrompts must be positive")
    int maxPrompts
) {
}
