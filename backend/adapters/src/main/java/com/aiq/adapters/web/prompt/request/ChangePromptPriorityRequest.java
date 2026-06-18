package com.aiq.adapters.web.prompt.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Request for changing prompt priority")
public record ChangePromptPriorityRequest(
    @Schema(description = "New prompt priority", example = "5", minimum = "0")
    @PositiveOrZero(message = "priority must be positive or 0")
    int priority
) {
}
